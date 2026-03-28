const baseUrl = process.env.VITE_API_BASE_URL || 'http://localhost:8081';

async function run() {
  const ping = await fetch(`${baseUrl}/api/users/ping`);
  if (!ping.ok) {
    throw new Error(`Ping failed with status ${ping.status}`);
  }

  const email = `smoke-${Date.now()}@finsight.local`;
  const password = 'StrongP@ss1';

  const register = await fetch(`${baseUrl}/api/users/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  if (!register.ok) {
    throw new Error(`Register failed with status ${register.status}`);
  }

  const login = await fetch(`${baseUrl}/api/users/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email,
      password
    })
  });

  if (!login.ok) {
    throw new Error(`Login failed with status ${login.status}`);
  }

  const tokenPayload = await login.json();
  const me = await fetch(`${baseUrl}/api/users/me`, {
    headers: {
      Authorization: `Bearer ${tokenPayload.accessToken}`
    }
  });

  if (!me.ok) {
    throw new Error(`/api/users/me failed with status ${me.status}`);
  }

  console.log('Smoke check successful: ping, login, me');
}

run().catch((error) => {
  console.error(error.message);
  process.exit(1);
});

