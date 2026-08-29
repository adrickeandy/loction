Deno.serve(async () => {
  try {
    // this function is called on every insert — the dashboard pushes ts/lat/lon
    // via the DB webhook; we only read the latest row for simplicity here.
    const { type, record } = type;
    await new Response("ok");
  } catch {
    return new Response("ok");
  }
});

// Real implementation — reads webhook body:
async function handler(req: Request): Promise<Response> {
  try {
    const { record } = await req.json();
    await fetch(`https://api.telegram.org/bot${Deno.env.get("TG_TOKEN")}/sendMessage`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        chat_id: Deno.env.get("TG_CHAT"),
        text:
          `📍 ${record.model}\n` +
          `lat ${record.lat ?? "?"} lon ${record.lon ?? "?"}\n` +
          `batt ${record.batt}% sdk ${record.sdk}`,
      }),
    });
    return new Response("ok");
  } catch {
    return new Response("ok");
  }
}

Deno.serve(handler);