const API_BASE = "http://localhost:8081/api/v1";
const TOKEN_KEY = "adminToken"; // Key for storing the JWT
let pollingInterval; // Global variable to hold the interval ID

document.addEventListener("DOMContentLoaded", () => {
  // 1. Check for token and redirect if missing
  const token = sessionStorage.getItem(TOKEN_KEY);
  if (!token) {
    // If no token, redirect to the login page (assuming it's at /login)
    window.location.href = "/login";
    return; // Stop execution
  }

  // 2. Start data fetching if token is present
  startPolling();
});

// --- CORE UTILITIES ---

/**
 * Handles all authenticated API requests by adding the Authorization header.
 * @param {string} url - The API endpoint URL.
 * @param {object} options - Fetch options (method, headers, body, etc.).
 * @returns {Promise<Response>}
 */
async function authenticatedFetch(url, options = {}) {
    const token = sessionStorage.getItem(TOKEN_KEY);

    // Add Authorization header
    options.headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
        ...options.headers // Merge existing headers
    };

    const response = await fetch(url, options);

    // Check for authentication failure (401 or 403)
    if (response.status === 401 || response.status === 403) {
        console.error("Authentication failed. Token may be expired or invalid.");
        sessionStorage.removeItem(TOKEN_KEY);
        // Redirect to login page
        window.location.href = "/login";
        throw new Error("Unauthorized");
    }

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response;
}

/**
 * Logs out the admin by removing the token and redirecting.
 */
function logout() {
    sessionStorage.removeItem(TOKEN_KEY);
    window.location.href = "/login?logout"; // Redirect to login
}
// Make logout function globally accessible via the window object (if needed from HTML)
window.logout = logout;


// --- DASHBOARD LOGIC (Updated to use authenticatedFetch) ---

function startPolling() {
    // Clear any existing interval first
    if (pollingInterval) {
        clearInterval(pollingInterval);
    }

    // Run the initial fetch immediately
    fetchDashboardData();

    // Set up the interval to run every 5000 milliseconds (5 seconds)
    pollingInterval = setInterval(fetchDashboardData, 5000);
}

function fetchDashboardData() {
    loadTransactions();
    loadWithdrawals();
    loadUsers();
    loadWallets();
}

function showSection(id) {
  document.querySelectorAll(".content-section").forEach(sec => sec.classList.remove("active"));
  document.getElementById(id).classList.add("active");

  document.querySelectorAll(".sidebar li").forEach(li => li.classList.remove("active"));
  // Use event.currentTarget instead of event.target for better reliability on the <li>
  event.currentTarget.classList.add("active");

  document.getElementById("section-title").textContent = id.charAt(0).toUpperCase() + id.slice(1);
}
// Make showSection function globally accessible from the HTML
window.showSection = showSection;


// 🧾 Fetch Transactions
async function loadTransactions() {
  try {
    const res = await authenticatedFetch(`${API_BASE}/transactions`);
    const data = await res.json();

    const tbody = document.getElementById("transactions-body");
    tbody.innerHTML = data.map(t => `
      <tr>
        <td>${t.id}</td>
        <td>${t.userId}</td>
        <td>${t.amount}</td>
        <td>${t.status}</td>
        <td>${new Date(t.createdAt).toLocaleString()}</td>
      </tr>
    `).join('');
  } catch (err) {
    if (err.message !== "Unauthorized") {
        console.error("Error loading transactions:", err);
        document.getElementById("transactions-body").innerHTML = '<tr><td colspan="5" class="error-message">Could not load transactions.</td></tr>';
    }
  }
}

// 💸 Fetch Withdrawals
async function loadWithdrawals() {
  try {
    // Note: Assuming a 'pending' endpoint or filtering is handled on the backend
    const res = await authenticatedFetch(`${API_BASE}/withdrawals?status=PENDING`);
    const data = await res.json();

    const tbody = document.getElementById("withdrawals-body");
    tbody.innerHTML = data.map(w => `
      <tr>
        <td>${w.id}</td>
        <td>${w.userId}</td>
        <td>${w.amount}</td>
        <td class="action-cell">
          <button class="approve" onclick="updateWithdrawal('${w.id}', 'APPROVED')">Approve</button>
          <button class="reject" onclick="updateWithdrawal('${w.id}', 'REJECTED')">Reject</button>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    if (err.message !== "Unauthorized") {
        console.error("Error loading withdrawals:", err);
        document.getElementById("withdrawals-body").innerHTML = '<tr><td colspan="4" class="error-message">Could not load withdrawals.</td></tr>';
    }
  }
}
// Make updateWithdrawal function globally accessible
window.updateWithdrawal = updateWithdrawal;

async function updateWithdrawal(id, status) {
  try {
    await authenticatedFetch(`${API_BASE}/withdrawals/${id}`, {
      method: "PUT",
      body: JSON.stringify({ status })
      // Content-Type: application/json is automatically added in authenticatedFetch
    });
    fetchDashboardData(); // Refresh data immediately
  } catch (err) {
    if (err.message !== "Unauthorized") {
        console.error("Error updating withdrawal:", err);
        // Optionally show a failure message to the admin
    }
  }
}

// 👥 Fetch Users
async function loadUsers() {
  try {
    const res = await authenticatedFetch(`${API_BASE}/users`);
    const data = await res.json();

    const tbody = document.getElementById("users-body");
    tbody.innerHTML = data.map(u => `
      <tr>
        <td>${u.userId}</td>
        <td>${u.phoneNumber}</td>
        <td>${u.wallet?.id || 'N/A'}</td>
      </tr>
    `).join('');
  } catch (err) {
    if (err.message !== "Unauthorized") {
        console.error("Error loading users:", err);
        document.getElementById("users-body").innerHTML = '<tr><td colspan="3" class="error-message">Could not load users.</td></tr>';
    }
  }
}

// 💼 Fetch Wallets
async function loadWallets() {
  try {
    const res = await authenticatedFetch(`${API_BASE}/wallets`);
    const data = await res.json();

    const tbody = document.getElementById("wallets-body");
    tbody.innerHTML = data.map(w => `
      <tr>
        <td>${w.id}</td>
        <td>${w.userId}</td>
        <td>${w.balance.toFixed(2)}</td>
      </tr>
    `).join('');
  } catch (err) {
    if (err.message !== "Unauthorized") {
        console.error("Error loading wallets:", err);
        document.getElementById("wallets-body").innerHTML = '<tr><td colspan="3" class="error-message">Could not load wallets.</td></tr>';
    }
  }
}