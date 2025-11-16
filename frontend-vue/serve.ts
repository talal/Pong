import { serve } from "bun";

serve({
  port: 4173,
  async fetch(req) {
    const url = new URL(req.url);
    let path = url.pathname;

    // Default to index.html at root
    if (path === "/") {
      path = "/index.html";
    }

    const file = Bun.file(`./dist${path}`);

    if (await file.exists()) {
      return new Response(file);
    }

    // SPA fallback: for Vue Router routes
    const index = Bun.file("./dist/index.html");
    if (await index.exists()) {
      return new Response(index, { status: 200 });
    }

    return new Response("Not found", { status: 404 });
  },
});
