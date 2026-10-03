import { configDefaults, defineConfig } from "vitest/config";

// Resolves the `@/*` alias from tsconfig.json, as Next.js does for the app.
export default defineConfig({
  resolve: { tsconfigPaths: true },
  test: {
    exclude: [...configDefaults.exclude, "tests/e2e/**"],
  },
});
