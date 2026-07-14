const { once } = require('node:events');
const { createServer } = require('node:http');

async function request(app, path, options = {}) {
  const server = createServer(app);
  server.listen(0);
  await once(server, 'listening');

  const { port } = server.address();
  const url = new URL(path, `http://127.0.0.1:${port}`);

  const headers = {
    ...(options.headers || {})
  };

  if (options.body) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(url, {
    method: options.method || 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  const body = await response.text();
  let parsedBody;

  try {
    parsedBody = body ? JSON.parse(body) : null;
  } catch {
    parsedBody = body;
  }

  server.close();
  return { response, body: parsedBody };
}

module.exports = { request };
