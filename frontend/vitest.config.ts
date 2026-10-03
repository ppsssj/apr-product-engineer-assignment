import { defineConfig } from "vitest/config";

// Resolves the `@/*` alias from tsconfig.json, as Next.js does for the app.
export default defineConfig({
  resolve: { tsconfigPaths: true },
});
