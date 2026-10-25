# Pages

One component per route. `App.jsx` chooses the page from the path and the user's role, and wraps it in `AppShell`.

| Route | Page | Who | What it does |
|---|---|---|---|
| `/` | `LandingPage` | Signed out | Marketing page |
| `/login` | `AuthPage` | Signed out | Sign in, or register as a customer or organiser |
| `/browse` | `BrowseEventsPage` | Signed in | Published events with search, category and student filters; loading, empty and error states |
| `/event/{id}` | `EventDetailPage` | Signed in | Live seat map: hold and release seats with a countdown, polling, student pricing |
| `/checkout` | `CheckoutPage` | Customers | Order summary; mock or Stripe payment; confirmation with e-tickets |
| `/tickets` | `MyTicketsPage` | Customers | Confirmed bookings and their QR tickets |
| `/events` | `EventsPage` | Organisers | Create events (optionally from a saved venue); set pricing (`EventPricingModal`); publish (`PublishEventModal`); delete |
| `/floorplans` | `FloorplansPage` | Organisers | Saved plans, starter templates and the Community Made gallery (preview, add a copy) |
| `/floorplan` | `FloorPlanEditorPage` | Organisers | Drag-and-drop builder for seats, tables, stage and barriers; save and publish to the gallery |
| `/venues` | `VenuesPage` | Organisers | Add, edit and delete venues (read-only for pending organisers) |
| `/admin` | `AdminDashboardPage` | Admin | Revenue, popular events, organiser and student-verification queues |

Page-specific styles sit beside each page (`EventDetailPage.css`, …). Tests (`*.test.jsx`) render the page with `RouterProvider` and `AuthProvider` and mock `src/api`.
