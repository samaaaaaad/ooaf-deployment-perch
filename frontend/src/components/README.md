# Components

Reusable pieces shared by several pages.

| Component | Used by | Purpose |
|---|---|---|
| `AppShell` | All signed-in pages | Header navigation (role-aware links), account menu, organiser-request and student-verification banners |
| `LoadingState` | Lists and pages | Spinner with `role="status"` |
| `EmptyState` | Lists | Title and hint when there is nothing to show |
| `ConfirmDialog` | AppShell, Admin dashboard | Accessible yes/no modal for destructive actions |
| `EventPricingModal` | EventsPage | Pick a floor plan, set standard and premium prices, student options |
| `PublishEventModal` | EventsPage | Choose or upload a banner, then publish |
| `FloorPlanPreview` | EventPricingModal, FloorPlanViewModal | Small SVG preview (`StagePreview` / `CustomPreview`) |
| `FloorPlanViewModal` | FloorplansPage | Full-size preview of a shared plan, with "Add to My Floor Plans" |
| `StudentVerificationModal` | AppShell | Apply for student verification with a card photo |
| `BookingTickets` | CheckoutPage, MyTicketsPage | Renders a booking's QR e-tickets |

## Conventions

- Components receive data and callbacks as props. Any API calls go through `src/api`.
- Show failures in an element with `role="alert"` so tests and screen readers can find them.
