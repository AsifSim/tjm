# 📖 UVM Protocol & TJM Sequencer API Documentation

Welcome to the **Unified Virtual Model (UVM) Orchestration Engine** API documentation. 

This system allows you to dynamically build, configure, and instantly deploy new API endpoints without writing backend code or restarting the server. It uses a **Transaction Journey Map (TJM)** to orchestrate complex data fetches and business rules step-by-step.

---

## 🌍 Base Information

*   **UVM Engine Base URL (Builder & Execution):** `http://localhost:8080`
*   **TML Controller Base URL (Internal Data Engine):** `http://localhost:8087`
*   **Content-Type:** `application/json`

---

## 🔐 1. Authentication
The UVM Engine supports two types of authentication depending on how you are interacting with it.

### A. UI / Web Dashboard Authentication (Bearer Token)
Used by the frontend dashboard to manage and build configurations.

*   **Endpoint:** `POST /api/v1/uvm/auth/signin`
*   **Request Body:**
    ```json
    {
      "username": "asif",
      "password": "your_password"
    }
    ```
*   **Response:** Returns a Bearer token. Include this in the header of subsequent builder requests: `Authorization: Bearer <token>`

### B. CLI / Console Authentication (Session Cookie)
Used when testing or triggering deployed dynamic APIs directly from a terminal or script.

*   **Endpoint:** `GET /api/v1/uvm/auth/cli-connect`
*   **Usage (cURL):** Use the `-c` flag to save the session cookie locally.
    ```bash
    curl -s -c uvm.cookie -u asif:your_password -X GET "http://localhost:8080/api/v1/uvm/auth/cli-connect"
    ```
*   **Result:** Creates a `uvm.cookie` file in your directory.

---

## 🗂 2. Metadata API

### Fetch Available Entities (DEAM)
Retrieves the list of all active data entities and their available columns (attributes) from the database. Use this to populate dropdowns in the UI builder.

*   **Endpoint:** `GET /api/v1/uvm/entities`
*   **Auth Required:** Yes (Bearer Token)
*   **Response:**
    ```json
    [
      {
        "entityName": "dashboard_layers",
        "attributes": [
          "dashboard_id",
          "layer_name",
          "layer_json"
        ]
      },
      {
        "entityName": "user_profiles",
        "attributes": [
          "user_id",
          "email",
          "role"
        ]
      }
    ]
    ```

---

## 🏗 3. Configuration Builder API

### Deploy a Dynamic API
This is the core endpoint. It takes your visual workflow, saves it as a JSON contract, and **instantly activates** the new API route in the server.

*   **Endpoint:** `POST /api/v1/uvm/config`
*   **Auth Required:** Yes (Bearer Token)
*   **Request Payload Breakdown:**

The payload wraps the API configuration inside an `apis` object, keyed by an alias (e.g., `calculatePrice`). The actual logic lives inside the `tjmSequence` object.

```json
{
  "appId": "APP-9000",
  "dashboardName": "Pricing Engine",
  "apis": {
    "calculatePrice": {
      "apiUrl": "api/v2/pricing",
      "method": "POST",
      "tjmSequence": {
        "inputParameters": "t1,t2",
        "sourceTrigger": "CONSOLE",
        "finalOutputEntity": "dashboard_layers",
        "finalOutputColumn": "layer_name",
        "finalResponseMappings": [
          {
            "targetCol": "dashboard_id",
            "operator": "=",
            "sourceInput": "dashboard_layers.dashboard_id"
          }
        ],
        "steps": [
          {
            "stepType": "FABRIC_FETCH",
            "targetEntity": "dashboard_layers",
            "targetColumns": "dashboard_id,layer_name",
            "stepMappings": [
              {
                "targetCol": "dashboard_id",
                "operator": "=",
                "sourceInput": "t1"
              }
            ]
          }
        ]
      }
    }
  }
}
```

### 🧠 Understanding the `tjmSequence` Contract
To build UIs or integrations, you must understand these fields:

*   `inputParameters`: Comma-separated list of keys the dynamic API expects in its request body (e.g., `"t1,t2"`).
*   `sourceTrigger`: The environment triggering the API (`CONSOLE`, `UI`, `EXCEL`).
*   `steps`: An array of actions executed in order.
    *   `targetEntity`: The database table to query.
    *   `stepMappings`: How to filter this query. `sourceInput` maps a runtime variable (like `t1`) to a physical database column (`targetCol`).
    *   **The Context Pool:** When a step finishes, the data it fetches is saved into memory so later steps can use it. It saves variables as `EntityName.ColumnName` (e.g., `dashboard_layers.dashboard_id`).
*   `finalResponseMappings`: The filters used for the *very last* query to generate the final response sent back to the user.
*   `finalOutputEntity` & `finalOutputColumn`: What specific data point should be returned to the client at the end of the journey.

---

## ⚡ 4. Dynamic API Execution

Once an API is deployed using the Builder API, it is immediately live at the `apiUrl` you defined (e.g., `/api/v2/pricing`). 

### Triggering your Custom API
*   **Endpoint:** `POST /<your-custom-url>` (e.g., `POST /api/v2/pricing`)
*   **Auth Required:** Yes (Session Cookie via CLI, or Bearer via UI)
*   **Request Body:** Must match the `inputParameters` you defined.

**cURL Example:**
Notice the `-b uvm.cookie` flag, which proves you authenticated in Step 1B.
```bash
curl -s -b uvm.cookie -X POST "http://localhost:8080/api/v2/pricing" \
     -H "Content-Type: application/json" \
     -d "{\"t1\": \"account_summary_managel_level_dup_ekx1hf\", \"t2\": \"sample_val\"}"
```

**Response Example (Success):**
The engine returns the orchestrated data wrapped in a standard UVM response envelope, including a trace ID for debugging.
```json
{
  "mode": "CONSOLE",
  "status": "SUCCESS",
  "data": {
    "layer_name": [
      {
        "layer_name": "Text"
      }
    ]
  },
  "trace_id": "f034a2ed-3aad-43a0-a0dd-02ad9f16d824",
  "timestamp": "2026-10-26T09:58:21.562Z"
}
```

---
**Need Help?** Check the UVM Engine backend logs (Port 8080) for `[TJM_ENGINE]` entries to trace how variables are being mapped and passed to the TML Controller during execution.