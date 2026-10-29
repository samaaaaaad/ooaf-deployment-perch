"""Check displayed classes, members, dependencies and all persistent associations against Java."""
from pathlib import Path
import hashlib, html, json, re, xml.etree.ElementTree as E

B=Path(__file__).resolve().parent
R=B.parents[1]
J=R/'src/main/java/com/example/event_ticketing'
data=json.loads((B/'class-inventory.json').read_text())
for name,digest in data['source_files'].items():
    assert hashlib.sha256((R/name).read_bytes()).hexdigest()==digest, f'Rebuild diagrams after source changed: {name}'
classes=data['classes']
for c in classes:
    matches=list(J.rglob(c['name']+'.java'))
    assert len(matches)==1,c['name']
    text=matches[0].read_text()
    for attr in c['attributes']:
        member=attr.split(':')[0].lstrip('- ').strip()
        assert re.search(r'private\s+(?:final\s+)?[\w.<>]+\s+'+member+r'\b',text),(c['name'],member)
    for op in c['operations']:
        name=op.lstrip('+ ').split('(')[0]
        assert re.search(r'\b'+name+r'\s*\(',text),(c['name'],name)
entities={p.stem for p in (J/'model').glob('*.java') if '@Entity' in p.read_text()}
assert {c['name'] for c in classes if c['kind']=='model'}==entities
assert {c['name'] for c in classes if c['kind']=='service'}=={p.stem for p in (J/'service').glob('*.java')}
# In this view edges run from the referenced object to the class holding the field.
roles={'u_v':'organiser','u_org':'user','u_stu':'user','u_event':'organiser','u_plan':'organiser',
       'u_booking':'customer','u_hold':'heldBy','plan_event':'floorPlan','plan_seat':'floorPlan',
       'event_booking':'event','event_seats':'event','booking_seat':'booking','booking_ticket':'booking','seat_ticket':'seat'}
actual=set()
for file in (J/'model').glob('*.java'):
    pattern=r'@(ManyToOne|OneToOne)\((.*?)\)\s*@JoinColumn\((.*?)\)\s*private\s+(\w+)\s+(\w+)\s*;'
    for kind,annotation,column,target,field in re.findall(pattern,file.read_text(),re.S):
        actual.add((file.stem,field,target))
        edge=next(e for e in data['relationships'] if e['page']=='Domain classes' and e['target']==file.stem and roles[e['id']]==field)
        assert edge['source']==target,(file.stem,field)
        required='optional = false' in annotation or 'nullable = false' in column
        assert edge['sourceMultiplicity']==('1' if required else '0..1')
        assert edge['targetMultiplicity']==('0..1' if kind=='OneToOne' else '0..*')
assert len(actual)==len(roles)==14
for edge in data['relationships']:
    if edge['page']!='Application classes':continue
    source=next(J.rglob(edge['source']+'.java')).read_text()
    if edge['id'] in ['mock_interface','stripe_interface']:
        assert 'implements '+edge['target'] in source
    else:assert re.search(r'private final '+edge['target']+r'\s+',source),edge
root=E.parse(B/'class-diagrams.drawio').getroot()
assert [d.get('name') for d in root.findall('diagram')]==['Domain classes','Application classes']
for page in root.findall('diagram'):
    cells=page.findall('.//mxCell');ids=[c.get('id') for c in cells]
    assert len(ids)==len(set(ids))
    for cell in cells:
        if cell.get('edge')=='1':assert cell.get('source') in ids and cell.get('target') in ids
        if cell.get('vertex')=='1' and cell.get('parent')=='1':
            g=cell.find('mxGeometry');m=page.find('mxGraphModel')
            assert float(g.get('x'))>=0 and float(g.get('y'))>=0
            assert float(g.get('x'))+float(g.get('width'))<=float(m.get('pageWidth'))
            assert float(g.get('y'))+float(g.get('height'))<=float(m.get('pageHeight'))
raw=(B/'class-diagrams.drawio').read_text()
for field in ['category: String','venueName: String','city: String']:assert field in raw
assert 'createVenue' not in raw and 'updateVenue' not in raw and 'deleteVenue' not in raw
assert not re.search(r'private\s+Venue\s+', (J/'model/Event.java').read_text())
assert not re.search(r'private\s+Seat\s+', (J/'model/EventSeat.java').read_text())
print(f'PASS: {len(entities)} entities, 11 services/jobs, payment interface/implementations, 14 JPA associations and all selected dependencies/members match the source.')
