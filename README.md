# MatrixCalculator - Assignment 3

## Overview
This repository contains the implementation for Assignment 3, which consists of two questions:
1. **Matrix Calculator**: An app to perform matrix addition and subtraction with dynamically generated input fields based on user-specified dimensions.
2. **WiFi RSS Logger**: An app to log WiFi RSSI values for three locations, display them in a 10x10 matrix, and compare the RSSI range across locations.

The project is built using Android Studio with Kotlin, and the app runs on Android devices (tested on API 33+).

---

## 1: Matrix Calculator

### Overview
The Matrix Calculator app allows users to perform matrix addition and subtraction. Users can specify the dimensions of the matrices (number of rows and columns), and the app dynamically generates input fields for the matrix elements. The result of the operation is displayed below the input fields.

### Features
- Input the number of rows and columns for the matrices using `EditText` fields.
- Click "Generate Matrix Fields" to dynamically create `EditText` fields for Matrix A and Matrix B based on the specified dimensions.
- Perform matrix addition or subtraction using "Add" and "Subtract" buttons.
- Display the result matrix in a `TextView` below the buttons.
- Input validation to ensure:
  - Both matrices have the same dimensions for addition and subtraction.
  - All input fields contain valid numbers.
- Navigation to the WiFi RSS Logger (Question 2) via an "Open WiFi Logger" button.

### How to Use
1. Launch the app to access the Matrix Calculator (main activity).
2. Enter the number of rows and columns for the matrices in the provided `EditText` fields (e.g., 2 rows, 2 columns for 2x2 matrices).
3. Click "Generate Matrix Fields" to create input fields for Matrix A and Matrix B.
4. Enter the elements of Matrix A and Matrix B in the generated `EditText` fields.
5. Click "Add" to perform matrix addition or "Subtract" to perform matrix subtraction.
   - Both matrices must have the same dimensions for these operations.
6. The result matrix is displayed in the `TextView` below the buttons with the label "Result will appear here".
7. Click "Open WiFi Logger" to navigate to the WiFi RSS Logger activity (Question 2).

### Implementation Details
- **Matrix Dimensions Input**: Two `EditText` fields capture the number of rows and columns.
- **Dynamic Input Fields**: Clicking "Generate Matrix Fields" creates a grid of `EditText` fields for Matrix A and Matrix B using a `LinearLayout` or similar layout.
- **Operations**:
  - Addition: Element-wise addition of two matrices.
  - Subtraction: Element-wise subtraction of two matrices.
  - Multiplication: Element-wise multiplication of two matrices.
- **Error Handling**:
  - Validates that both matrices have the same dimensions.
  - Ensures all input fields contain valid numbers, displaying a `Toast` message if invalid.
- **Result Display**: The result matrix is shown as a formatted string in a `TextView`.
- **Navigation**: The "Open WiFi Logger" button starts the `WifiLoggingActivity` for Question 2.

---

## 2: WiFi RSS Logger

### Overview
The WiFi RSS Logger app logs Received Signal Strength Indicator (RSSI) values for WiFi access points at three different locations (Room A, Room B, Corridor). It fulfills the following requirements:
- Logs 100 RSSI values (10x10 matrix) per location.
- Displays the matrix for the selected location with color-coding.
- Shows a summary table comparing the RSSI range (min, max, average) across all three locations.
- Saves data to a CSV file for persistence.

### Features
- Select a location (Room A, Room B, Corridor) using a dropdown.
- Start logging WiFi RSSI values with a button (10 scans per location, each scan logging 10 RSSI values).
- Display a 10x10 matrix of RSSI values for the selected location, with color-coding:
  - Green: RSSI ≥ -50 dBm
  - Yellow: RSSI ≥ -70 dBm
  - Red: RSSI < -70 dBm
- Display a summary table showing the minimum, maximum, and average RSSI for each location.
- Save the logged data to `wifi_rss_data.csv` in the app’s internal storage.
- Load data from the CSV on app startup to avoid re-scanning.
- Clear data (reset in-memory data and delete the CSV) with a button.

### How to Use
1. Launch the app and click **"Open WiFi Logger"** from the Matrix Calculator screen to access the WiFi RSS Logger.
2. Select a location (Room A, Room B, or Corridor) from the spinner.
3. Click **"Start Logging WiFi RSS"** to collect 10 scans for the selected location.
   - Each scan logs the RSSI values of up to 10 access points (padded with -80 dBm if fewer than 10 APs are detected).
   - Scans are performed every 30 seconds to comply with Android’s scan throttling (total time: 5 minutes per location).
4. View the 10x10 matrix of RSSI values for the selected location, color-coded as described above.
5. The summary table below the matrix shows the RSSI range (min, max, average) for all three locations.
6. Click **"Clear Data"** to reset the app and delete the CSV file.
7. To collect data for another location, select the new location and repeat the process.

### Accessing the Data
- The app saves data to `wifi_rss_data.csv` in the app's internal storage.
- To access the file:
  1. Open Android Studio.
  2. Go to **View > Tool Windows > Device File Explorer**.
  3. Navigate to `/data/data/mc.assignment3.matrixcalculator/files/`.
  4. Download `wifi_rss_data.csv`.
- The CSV file is also included in the `data/` folder of this repository for convenience (`data/wifi_rss_data.csv`).
- CSV Format: Timestamp,Location,ScanNumber,AP1,AP2,AP3,AP4,AP5,AP6,AP7,AP8,AP9,AP10


### Notes on Data
- The RSSI ranges are identical across all locations (Min: -80, Max: -50, Avg: -77) because the data was collected in the same physical location due to time constraints.
- To observe variation in RSSI ranges, collect data in different physical locations (e.g., closer to or farther from the WiFi access point).

### Implementation Details
- **Permissions**: Requests `ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, and `NEARBY_WIFI_DEVICES` to perform WiFi scans.
- **Scan Throttling**: Uses a 30-second interval between scans to comply with Android’s foreground scan limit (4 scans every 2 minutes).
- **Data Storage**: Saves data to `wifi_rss_data.csv` using `FileWriter` and loads it on startup using `FileReader`.
