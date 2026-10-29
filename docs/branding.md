# Perch branding assets

The three team-provided PNGs are stored unchanged in `frontend/public/branding/`.

| Asset | Use |
|---|---|
| `perch-app-icon.png` | Browser favicon, Apple home-screen icon and web manifest icon; 1024 × 1024 |
| `perch-logo-primary.png` | Full wordmark on light backgrounds: login and the signed-in app header; 2340 × 624 |
| `perch-logo-dark-backgrounds.png` | Yellow bird and yellow wordmark on dark backgrounds: landing navigation and footer; 2340 × 624 |

`BrandLogo` selects the primary artwork by default; use `<BrandLogo onDark />` for the supplied dark-background variant. The CSS preserves the original aspect ratio and does not recolour the artwork. The landing navigation uses a smaller wordmark at narrow widths so its action buttons still fit.

Original asset SHA-256 values:

```text
83c0a06fb25ddc67d6c9cd3f041773d4558277c3ae2829f24438b79479fcb619  perch-app-icon.png
45abfc39e907b652ce7a1c8089d1044dbe75aa789b0558bf22dec49e0afea112  perch-logo-dark-backgrounds.png
a6200061988462ad9ed38ee7508ba86499fcb399d9110a09ebba7e290769c18b  perch-logo-primary.png
```

The component tests check both variants, their placement on the actual pages, and the favicon/touch-icon/manifest references. The assets are copied into the production build by Vite.
