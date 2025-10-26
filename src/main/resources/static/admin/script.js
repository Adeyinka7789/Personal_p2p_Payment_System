const API_BASE = "http://localhost:8081/api/v1";
let pollingInterval; // Global variable to hold the interval ID

document.addEventListener("DOMContentLoaded", () => {
  // Start polling when the document is ready
  startPolling();
});

/**
 * Starts the data fetching interval.
 * Fetches all data immediately, then repeats every 5 seconds.
 */
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

/**
 * Calls all data loading functions.
 */
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

// 🧾 Fetch Transactions (Now called by polling)
async function loadTransactions() {
  try {
    const res = await fetch(`${API_BASE}/transactions`);
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    const data = await res.json();

    const tbody = document.getElementById("transactions-body");
    tbody.innerHTML = data.map(t => `
      <tr>
        <td>${t.id}</td>
        <td>${t.userId}</td>
        <td>${t.amount.toFixed(2)}</td>
        <td><span class="status ${t.status.toLowerCase()}">${t.status}</span></td>
        <td>${new Date(t.createdAt).toLocaleString()}</td>
      </tr>
    `).join('');
  } catch (err) {
    console.error("Error loading transactions (Is API running?):", err);
    document.getElementById("transactions-body").innerHTML = '<tr><td colspan="5" class="error-message">Could not connect to API.</td></tr>';
  }
}

// 💸 Fetch Withdrawals (Now called by polling)
async function loadWithdrawals() {
  try {
    const res = await fetch(`${API_BASE}/withdrawals?status=PENDING`);
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    const data = await res.json();

    const tbody = document.getElementById("withdrawals-body");
    tbody.innerHTML = data.map(w => `
      <tr>
        <td>${w.id}</td>
        <td>${w.userId}</td>
        <td>${w.amount.toFixed(2)}</td>
        <td>
          <button class="approve" onclick="updateWithdrawal('${w.id}', 'APPROVED')">Approve</button>
          <button class="reject" onclick="updateWithdrawal('${w.id}', 'REJECTED')">Reject</button>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    console.error("Error loading withdrawals:", err);
    document.getElementById("withdrawals-body").innerHTML = '<tr><td colspan="4" class="error-message">Could not connect to API.</td></tr>';
  }
}

async function updateWithdrawal(id, status) {
  try {
    const res = await fetch(`${API_BASE}/withdrawals/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status })
    });
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);

    // Reload all data after a successful update to show the change immediately
    fetchDashboardData();
  } catch (err) {
    console.error("Error updating withdrawal:", err);
  }
}

// 👥 Fetch Users (Now called by polling)
async function loadUsers() {
  try {
    const res = await fetch(`${API_BASE}/users`);
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
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
    console.error("Error loading users:", err);
    document.getElementById("users-body").innerHTML = '<tr><td colspan="3" class="error-message">Could not connect to API.</td></tr>';
  }
}

// 💼 Fetch Wallets (Now called by polling)
async function loadWallets() {
  try {
    const res = await fetch(`${API_BASE}/wallets`);
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
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
    console.error("Error loading wallets:", err);
    document.getElementById("wallets-body").innerHTML = '<tr><td colspan="3" class="error-message">Could not connect to API.</td></tr>';
  }
}