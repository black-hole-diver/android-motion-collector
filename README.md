# Android Motion Collector

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)

Android Motion Collector is an Android app designed to collect gyroscope, accelerometer, and gravity sensor data from a smartwatch and transfer it to a smartphone in CSV format with accurate timestamps. Furthermore, it gives prediction to the movement from a wearable to the phone in real-time. The application is used to primarily to compare sensor data output with iOS Motion Collector (Apple Watch 4) and utilized the trained TensorFlow lite model (Float32) to give real-time prediction. The application is one of the 2 applications used to finish the thesis of Wittawin Panta, Computer BSc, Eötvös Loránd University, 2023/2024-2. The thesis name is Machine Learning Based Real-time Movement Detection of Children.

## Features
- Collects data from gyroscope, accelerometer, and gravity sensors.
- Captures sensor data with precise timestamps.
- Exports data in CSV format.
- Give interence from the wearable to the phone in real-time.
- Lightweight and battery-efficient.

## Requirements
- Android Studio or equivalent IDE.
- A compatible Android smartphone.
- A compatible Android smartwatch.

## Installation

1. Clone this repository to your local machine:
    ```bash
    git clone https://github.com/black-hole-diver/android-motion-collector.git
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
