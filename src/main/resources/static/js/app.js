// Global references to Chart instances
let revenueChartInstance = null;
let topProductsChartInstance = null;
let productsList = [];

// Modals
let auditLogModal = null;
let adjustStockModal = null;
let addProductModal = null;
let simpleAuditModal = null;

document.addEventListener("DOMContentLoaded", () => {
  // Initialize Modals
  auditLogModal = new bootstrap.Modal(document.getElementById('auditLogModal'));
  adjustStockModal = new bootstrap.Modal(document.getElementById('adjustStockModal'));
  addProductModal = new bootstrap.Modal(document.getElementById('addProductModal'));
  simpleAuditModal = new bootstrap.Modal(document.getElementById('auditModal'));

  // Event Listeners
  document.getElementById("catalogSearch").addEventListener("input", handleSearch);
  document.getElementById("btnSmartReorder").addEventListener("click", triggerSmartReorder);
  document.getElementById("adjustStockForm").addEventListener("submit", submitStockAdjustment);
  document.getElementById("addProductForm").addEventListener("submit", submitAddProduct);

  // Load initial dashboard metrics & catalog data
  initDashboard();
});

/**
 * Initializes the dashboard content by fetching product and order statistics.
 */
async function initDashboard() {
  try {
    await Promise.all([
      fetchDashboardMetrics(),
      loadProductCatalog(),
      loadCategoryDropdown()
    ]);
  } catch (err) {
    console.error("Error initializing dashboard: ", err);
  }
}

/**
 * Fetch all metrics and populate the top metric cards and group data for chart rendering.
 */
async function fetchDashboardMetrics() {
  try {
    const [productsRes, ordersRes] = await Promise.all([
      fetch('/api/products'),
      fetch('/api/orders')
    ]);

    if (!productsRes.ok || !ordersRes.ok) {
      throw new Error("Unable to retrieve stats data from backend endpoints.");
    }

    const products = await productsRes.json();
    const orders = await ordersRes.json();

    // Update stats card variables
    // 1. Total Products
    document.getElementById("activeProductsVal").innerText = products.length;

    // 2. Low Stock Alerts (LOW_STOCK or OUT_OF_STOCK)
    const lowStockCount = products.filter(p => p.status === 'LOW_STOCK' || p.status === 'OUT_OF_STOCK').length;
    const lowStockAlertVal = document.getElementById("lowStockVal");
    lowStockAlertVal.innerText = lowStockCount;
    if (lowStockCount > 0) {
      lowStockAlertVal.classList.add("text-warning");
      document.getElementById("lowStockSub").innerHTML = `<i class="fa-solid fa-circle-exclamation me-1"></i>${lowStockCount} items need restocking`;
    } else {
      lowStockAlertVal.classList.remove("text-warning");
      document.getElementById("lowStockSub").innerHTML = `<i class="fa-solid fa-circle-check me-1"></i>Stock levels optimal`;
    }

    // 3. Total Orders
    const completedOrders = orders.filter(order => order.status === 'COMPLETED');
    document.getElementById("totalOrdersVal").innerText = completedOrders.length;

    // 4. Total Sales (Grand Total of COMPLETED orders)
    const totalSales = completedOrders.reduce((sum, order) => sum + (order.grandTotal || 0), 0);
    document.getElementById("totalSalesVal").innerText = `₹${totalSales.toFixed(2)}`;

    // Group Orders for Weekly Revenue trends
    const salesMap = getLast7Days();
    const productQtyMap = {};

    orders.forEach(order => {
      if (order.status === 'COMPLETED') {
        const dateField = order.generatedAt || order.createdAt;
        if (dateField) {
          const dateStr = dateField.split('T')[0];
          if (salesMap[dateStr] !== undefined) {
            salesMap[dateStr] += (order.grandTotal || 0);
          }
        }
      }

      if (order.status === 'COMPLETED' || order.status === 'PENDING') {
        // Group top products
        if (order.items && Array.isArray(order.items)) {
          order.items.forEach(item => {
            productQtyMap[item.productName] = (productQtyMap[item.productName] || 0) + (item.quantity || 0);
          });
        }
      }
    });

    // Set chart type text (Live vs Demo)
    const liveProducts = Object.keys(productQtyMap).length > 0;

    document.getElementById("revenueChartType").innerText = "Live Data";
    document.getElementById("revenueChartType").className = "chart-badge text-success border-success";
    
    document.getElementById("topProductsChartType").innerText = liveProducts ? "Live Data" : "Demo Data";
    document.getElementById("topProductsChartType").className = liveProducts ? "chart-badge text-success border-success" : "chart-badge text-muted border-secondary";

    // Call chart renderer
    renderCharts(salesMap, products);

  } catch (err) {
    console.error("Error loading dashboard metrics: ", err);
    // Render fallback empty live data on initial fail
    renderCharts(getLast7Days(), []);
  }
}

/**
 * Instantiate Chart.js objects for sales revenue line/bar and top product distributions.
 */
function renderCharts(salesData, products) {
  // 1. Destroy existing charts if initialized to prevent memory leaks and hover bugs
  if (revenueChartInstance) {
    revenueChartInstance.destroy();
  }
  if (topProductsChartInstance) {
    topProductsChartInstance.destroy();
  }

  // Prepare Sales Data
  const salesKeys = Object.keys(salesData).sort();
  const salesLabels = salesKeys.map(k => {
    // Format date as DD-MMM
    const parts = k.split('-');
    if (parts.length === 3) {
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      return `${parts[2]} ${months[parseInt(parts[1]) - 1]}`;
    }
    return k;
  });
  const salesValues = salesKeys.map(k => salesData[k]);

  // Prepare Product Distribution Data
  const sortedProds = [...products]
    .sort((a, b) => (b.avgDailySales || 0) - (a.avgDailySales || 0))
    .slice(0, 5);
  const prodLabels = sortedProds.map(p => p.name);
  const prodValues = sortedProds.map(p => p.avgDailySales);

  // Create Revenue Trends Bar Chart (with beautiful gradients)
  const revCtx = document.getElementById('revenueChart').getContext('2d');
  const gradient = revCtx.createLinearGradient(0, 0, 0, 350);
  gradient.addColorStop(0, 'rgba(99, 102, 241, 0.4)');
  gradient.addColorStop(1, 'rgba(168, 85, 247, 0.02)');

  revenueChartInstance = new Chart(revCtx, {
    type: 'line',
    data: {
      labels: salesLabels,
      datasets: [{
        label: 'Sales Revenue (₹)',
        data: salesValues,
        backgroundColor: gradient,
        borderColor: '#6366f1',
        borderWidth: 3,
        fill: true,
        tension: 0.4,
        pointBackgroundColor: '#a855f7',
        pointBorderColor: '#0b0f19',
        pointBorderWidth: 2,
        pointRadius: 6,
        pointHoverRadius: 8
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#0f1422',
          titleColor: '#fff',
          bodyColor: '#fff',
          borderColor: 'rgba(255,255,255,0.08)',
          borderWidth: 1,
          padding: 12,
          cornerRadius: 8,
          font: { family: 'Inter' }
        }
      },
      scales: {
        x: {
          grid: { color: 'rgba(255, 255, 255, 0.05)' },
          ticks: { color: '#9ca3af', font: { family: 'Inter', size: 11 } }
        },
        y: {
          grid: { color: 'rgba(255, 255, 255, 0.05)' },
          ticks: { 
            color: '#9ca3af', 
            font: { family: 'Inter', size: 11 },
            callback: function(val) { return '₹' + val.toLocaleString('en-IN'); }
          }
        }
      }
    }
  });

  // Create Top Selling Products Doughnut Chart
  const prodCtx = document.getElementById('topProductsChart').getContext('2d');
  topProductsChartInstance = new Chart(prodCtx, {
    type: 'doughnut',
    data: {
      labels: prodLabels,
      datasets: [{
        data: prodValues,
        backgroundColor: [
          '#6366f1',
          '#a855f7',
          '#06b6d4',
          '#14b8a6',
          '#ec4899'
        ],
        borderColor: '#0e1320',
        borderWidth: 4,
        hoverOffset: 8
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: '#9ca3af',
            padding: 16,
            font: { family: 'Inter', size: 11 }
          }
        },
        tooltip: {
          backgroundColor: '#0f1422',
          titleColor: '#fff',
          bodyColor: '#fff',
          borderColor: 'rgba(255,255,255,0.08)',
          borderWidth: 1,
          padding: 12,
          cornerRadius: 8,
          font: { family: 'Inter' }
        }
      },
      cutout: '72%'
    }
  });
}

/**
 * Retrieve the catalog products array, build table rows dynamically and append them to table.
 */
async function loadProductCatalog() {
  try {
    const response = await fetch('/api/products');
    if (!response.ok) {
      throw new Error("Unable to fetch products catalog from `/api/products`.");
    }

    productsList = await response.json();
    renderProductTable(productsList);

  } catch (err) {
    console.error("Error loading products: ", err);
    const tbody = document.getElementById("productTableBody");
    tbody.innerHTML = `
      <tr>
        <td colspan="7" class="text-center py-5 text-danger fw-semibold">
          <i class="fa-solid fa-circle-exclamation me-2"></i>Failed to fetch catalog. Please verify server connection.
        </td>
      </tr>
    `;
  }
}

/**
 * Render the product table with filtered data list.
 */
function renderProductTable(products) {
  const tbody = document.getElementById("productTableBody");
  document.getElementById("tableCountBadge").innerText = products.length;

  if (products.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="7" class="text-center py-5 text-muted">
          No products found matching filters.
        </td>
      </tr>
    `;
    return;
  }

  let rowsHtml = "";
  products.forEach(product => {
    // Dynamic status badge color setup
    let badgeClass = "badge-in-stock";
    let statusText = "In Stock";
    let statusIcon = "fa-circle-check";

    if (product.status === 'LOW_STOCK') {
      badgeClass = "badge-low-stock";
      statusText = "Low Stock";
      statusIcon = "fa-circle-exclamation";
    } else if (product.status === 'OUT_OF_STOCK') {
      badgeClass = "badge-out-of-stock";
      statusText = "Out of Stock";
      statusIcon = "fa-triangle-exclamation";
    }

    const priceFormatted = `₹${parseFloat(product.price).toFixed(2)}`;

    rowsHtml += `
      <tr>
        <td class="fw-semibold text-muted">#${product.id}</td>
        <td class="fw-semibold text-light">${escapeHtml(product.name)}</td>
        <td><span class="text-muted small">${escapeHtml(product.categoryName || 'General')}</span></td>
        <td class="fw-semibold">${priceFormatted}</td>
        <td>
          <span class="me-2">${product.stockQuantity}</span>
          <span class="text-muted small">(Min: ${product.reorderThreshold})</span>
        </td>
        <td>
          <span class="status-badge ${badgeClass}">
            <i class="fa-solid ${statusIcon}"></i>${statusText}
          </span>
        </td>
        <td>
          <button class="btn btn-secondary-custom btn-sm me-2" onclick="viewAuditLog(${product.id}, '${escapeQuote(product.name)}')">
            <i class="fa-solid fa-clock-rotate-left"></i>Audit
          </button>
          <button class="btn btn-secondary-custom btn-sm" onclick="showAdjustStockModal(${product.id}, '${escapeQuote(product.name)}', ${product.stockQuantity})">
            <i class="fa-solid fa-plus-minus"></i>Adjust
          </button>
        </td>
      </tr>
    `;
  });

  tbody.innerHTML = rowsHtml;
}

/**
 * Triggers the local search row filtration upon typing.
 */
function handleSearch(event) {
  const query = event.target.value.toLowerCase().trim();
  if (!query) {
    renderProductTable(productsList);
    return;
  }

  const filtered = productsList.filter(product => {
    const nameMatch = product.name && product.name.toLowerCase().includes(query);
    const categoryMatch = product.categoryName && product.categoryName.toLowerCase().includes(query);
    const statusMatch = product.status && product.status.toLowerCase().replace('_', ' ').includes(query);
    return nameMatch || categoryMatch || statusMatch;
  });

  renderProductTable(filtered);
}

/**
 * Hit POST /api/reorder/trigger to draft smart purchase orders.
 */
async function triggerSmartReorder() {
  const btn = document.getElementById("btnSmartReorder");
  const originalText = btn.innerHTML;
  
  try {
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status"></span>Processing Reorder...`;

    const response = await fetch('/api/reorder/trigger', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    });

    if (!response.ok) {
      throw new Error("Reorder request trigger execution returned error code.");
    }

    // Pop up a clean browser alert upon success
    alert("Smart Reorder Engine ran successfully!\nRestock purchase orders have been drafted for low-stock items.");
    
    // Reload products and stats
    await initDashboard();

  } catch (err) {
    console.error("Error triggering smart reorder: ", err);
    alert("Failed to trigger automated reorder. Please verify server state.");
  } finally {
    btn.disabled = false;
    btn.innerHTML = originalText;
  }
}

/**
 * Fetch stock movement history and render a timeline inside the audit log modal.
 */
async function viewAuditLog(productId, productName) {
  try {
    document.getElementById("simpleAuditProductName").innerText = productName;
    document.getElementById("simpleAuditProductId").innerText = `#${productId}`;
    
    const tbody = document.getElementById("simpleAuditTableBody");
    tbody.innerHTML = `
      <tr>
        <td colspan="4" class="text-center py-4 text-muted">
          <div class="spinner-border spinner-border-sm text-indigo me-2" role="status"></div>
          Fetching audit trail...
        </td>
      </tr>
    `;
    
    simpleAuditModal.show();

    const response = await fetch(`/api/stock-audit/product/${productId}`);
    if (!response.ok) {
      throw new Error("Unable to retrieve stock logs.");
    }

    const logs = await response.json();
    
    if (logs.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="4" class="text-center py-4 text-muted">
            No stock movements logged for this product.
          </td>
        </tr>
      `;
      return;
    }

    let rowsHtml = "";
    logs.forEach(log => {
      // Format Date
      let dateFormatted = "Unknown Date";
      if (log.createdAt) {
        const date = new Date(log.createdAt);
        dateFormatted = date.toLocaleString('en-IN', {
          day: 'numeric',
          month: 'short',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        });
      }

      const sign = log.quantityChange >= 0 ? "+" : "";
      const changeText = `${sign}${log.quantityChange}`;
      const changeColorClass = log.quantityChange >= 0 ? "text-success" : "text-danger";

      rowsHtml += `
        <tr>
          <td>${dateFormatted}</td>
          <td><span class="badge bg-secondary">${escapeHtml(log.eventType || 'ADJUSTMENT')}</span></td>
          <td><span class="${changeColorClass} fw-bold">${changeText}</span></td>
          <td><span class="text-light opacity-75">${escapeHtml(log.remarks || 'No remarks provided')}</span></td>
        </tr>
      `;
    });
    tbody.innerHTML = rowsHtml;

  } catch (err) {
    console.error("Error viewing audit trail: ", err);
    document.getElementById("simpleAuditTableBody").innerHTML = `
      <tr>
        <td colspan="4" class="text-center py-4 text-danger">
          <i class="fa-solid fa-circle-exclamation me-2"></i>Failed to load audit trail logs.
        </td>
      </tr>
    `;
  }
}

/**
 * Show Modal form for Quick Stock Adjustment.
 */
function showAdjustStockModal(id, name, currentStock) {
  document.getElementById("adjustProductId").value = id;
  document.getElementById("adjustProductName").value = name;
  document.getElementById("adjustCurrentStock").value = `${currentStock} units`;
  document.getElementById("adjustQuantityChange").value = "";
  document.getElementById("adjustRemarks").value = "";
  adjustStockModal.show();
}

/**
 * Submits the Quick Stock Adjustment form via bulk stock endpoint.
 */
async function submitStockAdjustment(event) {
  event.preventDefault();
  
  const productId = parseInt(document.getElementById("adjustProductId").value);
  const quantityChange = parseInt(document.getElementById("adjustQuantityChange").value);
  const remarks = document.getElementById("adjustRemarks").value.trim();
  const btn = document.getElementById("btnSubmitStockAdjust");

  if (isNaN(quantityChange) || quantityChange === 0) {
    alert("Please specify a non-zero adjustment quantity.");
    return;
  }

  try {
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status"></span>Applying...`;

    const payload = [{
      productId: productId,
      quantityChange: quantityChange,
      remarks: remarks
    }];

    const response = await fetch('/api/products/bulk-stock-update', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      throw new Error("Stock update failed.");
    }

    adjustStockModal.hide();
    
    // Reload products and stats
    await initDashboard();

  } catch (err) {
    console.error("Error submitting stock adjustment: ", err);
    alert("Failed to submit stock adjustment. Please verify request values and try again.");
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<i class="fa-solid fa-check me-1"></i> Apply Adjustment`;
  }
}

// Helper to sanitize HTML strings
function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

// Helper to escape single quotes inside HTML attributes
function escapeQuote(str) {
  if (!str) return '';
  return str.replace(/'/g, "\\'");
}

/**
 * Dynamically fetch category data and load options into the selection dropdown.
 */
async function loadCategoryDropdown() {
  try {
    const response = await fetch('/api/categories');
    if (!response.ok) {
      throw new Error("Unable to fetch categories list.");
    }
    const categories = await response.json();
    const select = document.getElementById("addProductCategory");
    if (!select) return;
    
    // Retain only default disabled prompt option
    select.innerHTML = '<option value="" disabled selected>Select Category</option>';
    
    categories.forEach(category => {
      const option = document.createElement("option");
      option.value = category.id;
      option.textContent = category.name;
      select.appendChild(option);
    });
  } catch (err) {
    console.error("Error loading categories dropdown: ", err);
  }
}

/**
 * Submit handler for the Add New Product form.
 */
async function submitAddProduct(event) {
  event.preventDefault();
  
  const btn = document.getElementById("btnSubmitAddProduct");
  const originalText = btn.innerHTML;

  try {
    const name = document.getElementById("addProductName").value.trim();
    const description = document.getElementById("addProductDescription").value.trim();
    const price = parseFloat(document.getElementById("addProductPrice").value);
    const stockQuantity = parseInt(document.getElementById("addProductStock").value);
    const reorderThreshold = parseInt(document.getElementById("addProductThreshold").value);
    const avgDailySales = parseFloat(document.getElementById("addProductDailySales").value);
    const categoryIdVal = document.getElementById("addProductCategory").value;

    if (!categoryIdVal) {
      alert("Please select a category.");
      return;
    }
    const categoryId = parseInt(categoryIdVal);

    const payload = {
      name,
      description: description || null,
      price,
      stockQuantity,
      reorderThreshold,
      avgDailySales,
      categoryId
    };

    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status"></span>Adding...`;

    const response = await fetch('/api/products', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (response.status === 201 || response.ok) {
      alert("Product added successfully!");
      addProductModal.hide();
      document.getElementById("addProductForm").reset();
      
      // Seamlessly reload dashboard metrics and catalog table
      await initDashboard();
    } else {
      let errorMsg = `Failed to add product (Status ${response.status}).`;
      try {
        const errJson = await response.json();
        if (errJson.errors && Array.isArray(errJson.errors)) {
          errorMsg = errJson.errors.map(e => e.defaultMessage || e.message).join("\n");
        } else if (errJson.message) {
          errorMsg = errJson.message;
        }
      } catch (e) {
        // response not JSON
      }
      throw new Error(errorMsg);
    }
  } catch (err) {
    console.error("Error submitting add product form: ", err);
    alert("Error adding product:\n" + err.message);
  } finally {
    btn.disabled = false;
    btn.innerHTML = originalText;
  }
}

/**
 * Generates an empty sales map for the last 7 days.
 */
function getLast7Days() {
  const map = {};
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const dateStr = `${year}-${month}-${day}`;
    map[dateStr] = 0;
  }
  return map;
}
