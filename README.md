# Sensor Data Collector App

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)


<img src="https://github.com/black-hole-diver/mozgasmeres-wearos-1/blob/slicing-3000-ms/a-girl-biking.jpg" alt="drawing" width="300"/>

Sensor Data Collector is an Android app designed to collect gyroscope, accelerometer, and gravity sensor data from a smartwatch and transfer it to a smartphone in CSV format with accurate timestamps.

## Features
- Collects data from gyroscope, accelerometer, and gravity sensors.
- Captures sensor data with precise timestamps.
- Exports data in CSV format.
- Transfers sensor data from smartwatch to smartphone.
- Lightweight and battery-efficient.

## Requirements
- Android Studio or equivalent IDE.
- A compatible Android smartphone.
- A compatible Android smartwatch.

<img src="https://github.com/black-hole-diver/mozgasmeres-wearos-1/blob/main/MobileApp/src/main/res/mipmap-xxhdpi/ic_launcher_round.png" alt="icon-version-2" width="200"/>

## Installation

1. Clone this repository to your local machine:
    ```bash
    git clone https://github.com/black-hole-diver/mozgasmeres-wearos-1.git
    ```
2. Open the project in Android Studio.
3. Build and run the project on both the smartwatch and the smartphone.

## Usage
1. Connect Devices:
Make sure both the smartphone and smartwatch are paired via Bluetooth.
2. Launch Apps:
- Start the Sensor Data Collector app on phone.
3. Start Collection:
- Press the "Start" button on the phone to begin collecting data.
- Data is continuously sent to the smartphone and sent to Downloads/ directory on phone.

## Data Format
The exported CSV file contains the following columns:

1. Timestamp (milliseconds since epoch)
2. Accelerometer with X, Y, Z values (uacc_x, uacc_y, uacc_z)
3. Gravity with X, Y, Z values (grav_x, grav_y, grav_z)
4. Gyroscope with X, Y, Z values (gyr_x, gyr_y, gyr_z)
