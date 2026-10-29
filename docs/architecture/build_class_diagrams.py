"""Generate editable draw.io UML views from the reviewed branch inventory."""
from pathlib import Path
import xml.etree.ElementTree as E
import html
import json

OUT = Path(__file__).resolve().parent
SOURCE = OUT.parents[1]
import subprocess, hashlib
COMMIT = subprocess.check_output(['git', '-C', str(SOURCE), 'rev-parse', 'HEAD'], text=True).strip()
inventory = {'commit': COMMIT, 'classes': [], 'relationships': []}

class Page:
    def __init__(self, name, width, height):
        self.name, self.width, self.height = name, width, height
        self.model = E.Element('mxGraphModel', dx='1800', dy='1200', grid='1', gridSize='10',
            page='1', pageScale='1', pageWidth=str(width), pageHeight=str(height), math='0', shadow='0')
        self.root = E.SubElement(self.model, 'root')
        E.SubElement(self.root, 'mxCell', id='0')
        E.SubElement(self.root, 'mxCell', id='1', parent='0')
        self.n = 0
    def box(self, id, text, x, y, w, h, style='', parent='1'):
        c=E.SubElement(self.root,'mxCell',id=id,value=text,vertex='1',parent=parent,
            style='whiteSpace=wrap;html=1;fontFamily=Arial;fontSize=18;strokeColor=#334155;'+style)
        E.SubElement(c,'mxGeometry',x=str(x),y=str(y),width=str(w),height=str(h),attrib={'as':'geometry'})
        return id
    def text(self,id,text,x,y,w,h,size=18,align='left'):
        return self.box(id,text,x,y,w,h,f'text;strokeColor=none;fillColor=none;align={align};verticalAlign=middle;fontSize={size};')
    def title(self,text,sub):
        self.text('title',text,35,15,self.width-70,40,28)
        self.text('subtitle',sub,35,55,self.width-70,35,17)
    def cls(self,name,x,y,w,attrs,ops,kind='model',h=None):
        header = ('&lt;&lt;interface&gt;&gt;<br/>' if kind=='interface' else '')+name
        hh=58 if kind=='interface' else 38
        ah=max(36,len(attrs)*24+16)
        oh=max(38,len(ops)*24+16)
        h=h or hh+ah+oh
        self.box(name,'',x,y,w,h,'fillColor=#FFFFFF;')
        self.box(name+'_name',header,0,0,w,hh,'fillColor=#EDF3F7;fontStyle=1;align=center;',name)
        self.box(name+'_attributes','<br/>'.join(html.escape(t) for t in attrs),0,hh,w,ah,
            'fillColor=#FFFFFF;align=left;verticalAlign=top;spacingLeft=10;spacingTop=8;',name)
        self.box(name+'_operations','<br/>'.join(html.escape(t) for t in ops),0,hh+ah,w,h-hh-ah,
            'fillColor=#FFFFFF;align=left;verticalAlign=top;spacingLeft=10;spacingTop=8;',name)
        inventory['classes'].append({'name':name,'kind':kind,'attributes':attrs,'operations':ops,'page':self.name})
        return h
    def edge(self,id,src,dst,label='',style='',points=(),sm=None,tm=None):
        c=E.SubElement(self.root,'mxCell',id=id,value=label,edge='1',parent='1',source=src,target=dst,
            style='edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=none;strokeColor=#334155;strokeWidth=1.5;fontFamily=Arial;fontSize=17;labelBackgroundColor=#FFFFFF;'+style)
        g=E.SubElement(c,'mxGeometry',relative='1',attrib={'as':'geometry'})
        if points:
            a=E.SubElement(g,'Array',attrib={'as':'points'})
            for x,y in points:E.SubElement(a,'mxPoint',x=str(x),y=str(y))
        for suffix,val,xx,yy in [('s',sm,-.87,-13),('t',tm,.87,-13)]:
            if val:
                lab=E.SubElement(self.root,'mxCell',id=id+'_'+suffix,value=val,vertex='1',connectable='0',parent=id,
                    style='edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontFamily=Arial;fontSize=18;labelBackgroundColor=#FFFFFF;')
                geo=E.SubElement(lab,'mxGeometry',x=str(xx),y=str(yy),relative='1',attrib={'as':'geometry'})
                E.SubElement(geo,'mxPoint',x='0',y='0',attrib={'as':'offset'})
        inventory['relationships'].append({'id':id,'source':src,'target':dst,'sourceMultiplicity':sm,'targetMultiplicity':tm,'page':self.name})
    def note(self,id,text,x,y,w,h):
        self.box(id,text,x,y,w,h,'shape=note;fillColor=#FFFDF2;align=left;verticalAlign=top;spacing=12;fontSize=17;')

def save(name,pages):
    f=E.Element('mxfile',host='app.diagrams.net',version='26.0.0',type='device')
    for i,p in enumerate(pages):
        d=E.SubElement(f,'diagram',id='page'+str(i),name=p.name)
        d.append(p.model)
    E.indent(f)
    E.ElementTree(f).write(OUT/name,encoding='utf-8',xml_declaration=True)

d=Page('Domain classes',1810,1240)
d.title('Class diagram  |  A  Domain structure','Perch event ticketing • Selected attributes and operations • UML associations show stored object references')
d.cls('Venue',40,115,330,['- venueId: UUID','- name: String','- address: String','- city: String','- state: String'],['+ getName(): String'])
d.cls('User',470,115,330,['- userId: UUID','- fullName: String','- email: String','- passwordHash: String','- role: UserRole','- studentVerified: Boolean'],['+ isStudentVerified(): boolean'])
d.cls('OrganiserRequest',940,115,330,['- requestId: UUID','- status: OrganiserRequestStatus','- resolvedAt: Instant','- notified: boolean'],['+ setStatus(status): void'])
d.cls('StudentVerificationRequest',1410,115,360,['- requestId: UUID','- university: String','- studentNumber: String','- cardImage: String','- status: StudentVerificationStatus'],['+ resolve(outcome, notified): void'])
d.cls('FloorPlan',40,470,330,['- floorPlanId: UUID','- name: String','- layoutJson: String','- capacity: Integer','- shared: boolean'],['+ publish(): void','+ unpublish(): void'])
d.cls('Event',470,470,330,['- eventId: UUID','- title: String','- status: EventStatus','- startsAt: Instant','- standardPrice: BigDecimal','- premiumPrice: BigDecimal','- category: String','- venueName: String','- city: String','- studentDiscountPercent: Integer'],['+ hasStudentDiscount(): boolean'])
d.cls('Booking',940,470,330,['- bookingId: UUID','- status: BookingStatus','- totalAmount: BigDecimal','- paymentReference: String','- seatIds: List<UUID>'],['+ pricePaidFor(seat): BigDecimal'])
d.cls('Ticket',1410,470,360,['- ticketId: UUID','- ticketCode: String {unique}','- issuedAt: Instant'],['+ getTicketCode(): String'])
d.cls('Seat',40,880,330,['- seatId: UUID','- label: String','- tier: SeatTier','- x: Integer','- y: Integer'],['+ getTier(): SeatTier'])
d.cls('EventSeat',470,880,330,['- eventSeatId: UUID','- label: String','- price: BigDecimal','- status: SeatStatus','- version: Long','- holdExpiresAt: Instant'],['+ isHoldActiveAt(now): boolean','+ hold(customer, expiresAt): void','+ book(booking): void'])
d.edge('u_v','User','Venue','organiser',sm='1',tm='0..*',style='exitX=0;exitY=0.3;entryX=1;entryY=0.3;')
d.edge('u_org','User','OrganiserRequest','user',sm='1',tm='0..*',style='exitX=1;exitY=0.2;entryX=0;entryY=0.25;')
d.edge('u_stu','User','StudentVerificationRequest','user',sm='1',tm='0..*',points=[(850,95),(1360,95),(1360,210)],style='exitX=1;exitY=0.05;entryX=0;entryY=0.4;')
d.edge('u_event','User','Event','organiser',sm='1',tm='0..*',style='exitX=0.5;exitY=1;entryX=0.5;entryY=0;')
d.edge('u_plan','User','FloorPlan','organiser',sm='0..1',tm='0..*',points=[(425,380),(425,430),(205,430)],style='exitX=0;exitY=0.85;entryX=0.5;entryY=0;')
d.edge('u_booking','User','Booking','customer',sm='1',tm='0..*',points=[(855,410),(1105,410)],style='exitX=1;exitY=0.8;entryX=0.5;entryY=0;')
d.edge('u_hold','User','EventSeat','heldBy',sm='0..1',tm='0..*',points=[(875,345),(875,1000)],style='exitX=1;exitY=0.65;entryX=1;entryY=0.4;')
d.edge('plan_event','FloorPlan','Event','floorPlan',sm='0..1',tm='0..*',style='exitX=1;exitY=0.5;entryX=0;entryY=0.4;')
d.edge('plan_seat','FloorPlan','Seat','floorPlan',sm='1',tm='0..*',style='exitX=0.5;exitY=1;entryX=0.5;entryY=0;')
d.edge('event_booking','Event','Booking','event',sm='1',tm='0..*',style='exitX=1;exitY=0.3;entryX=0;entryY=0.4;')
d.edge('event_seats','Event','EventSeat','event',sm='1',tm='0..*',style='exitX=0.5;exitY=1;entryX=0.5;entryY=0;')
d.edge('booking_seat','Booking','EventSeat','booking',sm='0..1',tm='0..*',points=[(1105,815),(825,815),(825,1080)],style='exitX=0.5;exitY=1;entryX=1;entryY=0.7;')
d.edge('booking_ticket','Booking','Ticket','booking',sm='1',tm='0..*',style='exitX=1;exitY=0.4;entryX=0;entryY=0.5;')
d.edge('seat_ticket','EventSeat','Ticket','seat {unique}',sm='1',tm='0..1',points=[(635,1190),(1790,1190),(1790,610)],style='exitX=0.5;exitY=1;entryX=1;entryY=0.7;')
d.note('domain_notes','<b>Reading this view</b><br/>− private attribute; + public operation. Parameter types are abbreviated.<br/>Association-end numbers state how many instances may be related.<br/>Only selected members are shown; getters, setters and constructors are mostly omitted.<br/><br/><b>Implementation boundaries</b><br/>Roles are UserRole values, not User subclasses. A built-in FloorPlan may have no organiser. Event stores category, venueName and city as text; it has no Venue association. EventSeat has no stored Seat reference.<br/><br/>Pending/cancelled bookings may have no linked EventSeat or Ticket. Confirmation links the sold seats and issues one Ticket per seat. Booking.seatIds separately records the selected IDs before confirmation.',940,875,830,280)
for edge in d.root.findall("mxCell[@edge='1']"):
    edge.find('mxGeometry').set('y','18')

a=Page('Application classes',1810,1270)
a.title('Class diagram  |  B  Application structure','Perch event ticketing • Business operations and selected collaborators • Dashed arrows denote dependencies')
def svc(name,x,y,ops,attrs=(),w=365):return a.cls(name,x,y,w,list(attrs),ops,'service')
svc('UserService',40,115,['+ register(req): UserDTO','+ login(req): UserDTO'],['- userRepository: UserRepository'])
svc('OrganiserRequestService',490,115,['+ submit(userId): UserDTO','+ approve(requestId): OrganiserRequestDTO','+ reject(requestId): OrganiserRequestDTO'])
svc('StudentVerificationService',940,115,['+ submit(userId, body): StudentVerificationDTO','+ approve(requestId): StudentVerificationDTO','+ reject(requestId): StudentVerificationDTO'],w=400)
svc('AdminService',1410,115,['+ getStats(): AdminStatsDTO'],['- eventSeatRepository: EventSeatRepository'])
svc('VenueService',40,390,['+ getVenuesForOrganiser(organiserId): List<VenueDTO>','+ getVenueForOrganiser(organiserId, venueId): VenueDTO'],w=415)
svc('EventService',490,390,['+ createEvent(organiserId, request): EventDTO','+ publishEvent(organiserId, eventId): EventDTO','+ updateEventPricing(organiserId, eventId, request): EventDTO','+ createFloorPlan(organiserId, request): FloorPlanDTO','+ cloneSharedFloorPlan(organiserId, floorPlanId): FloorPlanDTO'],w=480)
svc('SeatHoldService',1050,390,['+ holdSeat(eventId, seatId, customerId): SeatHoldDTO','+ releaseHold(eventId, seatId, customerId): void','+ releaseExpiredHolds(): int'],w=430)
svc('ExpiredSeatHoldReleaseJob',1540,390,['+ releaseExpiredHolds(): void'],w=235)
svc('BookingService',40,770,['+ startCheckout(eventId, request): BookingDTO','+ confirmPaidBooking(paymentIntentId): BookingDTO','+ markPaymentFailed(paymentIntentId): void'],['- paymentProvider: PaymentProvider'],w=425)
svc('TicketService',560,770,['+ issue(booking, seats): void','+ getTickets(bookingId): List<BookingDTO.Ticket>','+ qrPng(ticketCode): byte[]'],w=365)
svc('BookingEmailService',1050,770,['+ sendBookingConfirmation(booking, seats): void'],['- mailSender: JavaMailSender [0..1]'],w=430)
a.cls('PaymentProvider',40,1050,425,[],['+ createPaymentIntent(...): PaymentIntentResult','+ autoConfirms(): boolean'],'interface')
a.cls('MockPaymentProvider',560,1080,365,[],['+ autoConfirms(): boolean'],'payment')
a.cls('StripePaymentProvider',1050,1080,430,['- client: StripeClient'],['+ createPaymentIntent(...): PaymentIntentResult'],'payment')
dep='dashed=1;endArrow=open;endFill=0;'
a.edge('job_hold','ExpiredSeatHoldReleaseJob','SeatHoldService','',dep+'exitX=0;exitY=0.5;entryX=1;entryY=0.5;')
a.edge('booking_ticketservice','BookingService','TicketService','',dep+'exitX=1;exitY=0.4;entryX=0;entryY=0.4;')
a.edge('booking_email','BookingService','BookingEmailService','',dep+'exitX=1;exitY=0.9;entryX=0;entryY=0.9;',points=[(500,1005),(1010,1005),(1010,900)])
a.edge('email_ticket','BookingEmailService','TicketService','',dep+'exitX=0;exitY=0.3;entryX=1;entryY=0.3;')
a.edge('booking_provider','BookingService','PaymentProvider','',dep+'exitX=0.5;exitY=1;entryX=0.5;entryY=0;')
a.edge('mock_interface','MockPaymentProvider','PaymentProvider','', 'dashed=1;endArrow=block;endFill=0;exitX=0;exitY=0.5;entryX=1;entryY=0.5;')
a.edge('stripe_interface','StripePaymentProvider','PaymentProvider','', 'dashed=1;endArrow=block;endFill=0;exitX=0.5;exitY=1;entryX=0.5;entryY=1;',points=[(1265,1245),(252,1245)])
a.note('app_notes','<b>Scope</b><br/>All services and the scheduled job are shown. Members are selective; … abbreviates parameters.<br/><br/>Controllers delegate to services; repositories persist the domain objects in panel A. DTOs cross HTTP boundaries.<br/><br/>Hollow triangles denote interface realisation. Mock is the default payment mode. Stripe confirms by webhook.<br/><br/>JavaMailSender is optional. Email is skipped without mail configuration.',40,620,1735,110)
# Replace long horizontal note with compact side note to avoid hiding service boxes.
note=a.root.find("mxCell[@id='app_notes']")
note.find('mxGeometry').set('x','1510');note.find('mxGeometry').set('y','650')
note.find('mxGeometry').set('width','265');note.find('mxGeometry').set('height','585')
note.set('style',note.get('style').replace('fontSize=17','fontSize=16'))



# Abbreviate parameters in the application view; full signatures remain in Java.
import re
for cell in a.root.findall('mxCell'):
    if cell.get('id','').endswith(('_operations','_attributes')):
        cell.set('style',cell.get('style')+'fontSize=16;')
    if cell.get('parent')=='ExpiredSeatHoldReleaseJob':
        cell.set('style',cell.get('style')+'fontSize=14;')
    if cell.get('id','').endswith('_operations'):
        cell.set('value',re.sub(r'\([^)]*\)',lambda m:'()' if m.group(0)=='()' else '(…)',cell.get('value','')))
save('class-diagrams.drawio',[d,a])
inventory['source_files']={str(p.relative_to(SOURCE)):hashlib.sha256(p.read_bytes()).hexdigest()
    for p in sorted((SOURCE/'src/main/java').rglob('*.java'))}
(OUT/'class-inventory.json').write_text(json.dumps(inventory,indent=2)+'\n')
print('Generated two editable class views and source inventory from '+COMMIT[:7]+'.')
