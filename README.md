# Elastic Agent Android (Unofficial)
[![Android CI](https://github.com/swiftbird07/elastic-agent-android/actions/workflows/android.yml/badge.svg)](https://github.com/swiftbird07/elastic-agent-android/actions/workflows/android.yml)
![Elastic Agent Android Logo](logo_elastic_agent.png)


Elastic Agent Android is an unofficial implementation of the Elastic Agent for Android devices, bringing the powerful observability and management features of the Elastic Stack to the Android ecosystem. With Elastic Agent Android, you can enroll your Android devices into a Fleet server and start collecting a wide range of data directly into Elasticsearch, allowing for real-time monitoring, alerting, and analysis.

## Use Cases

Elastic Agent Android aims to extend the powerful features of Elastic Observability and Security to mobile devices, providing detailed insights and security monitoring for Android devices. Whether you're managing a fleet of corporate devices or looking for a way to integrate mobile device data into your Elastic Stack, Elastic Agent Android offers a versatile and powerful solution.

## Features

Elastic Agent Android supports a variety of components that collect different types of data from the Android device, including:

- **Location:** Sends periodic location updates to Elasticsearch, with configurable intervals.
- **Network Logs:** Collects network logs (DNS, TCP connections) provided by the Android OS.
- **Security Logs:** Gathers security-related logs, like app (un-) installation, failed PIN attempts etc.

  
   >**Note**: Currently not working on any tested devices. See Issue [#01](https://github.com/swiftbird07/elastic-agent-android/issues/1).
- **Self Log:** Logs the agent's own operational logs for diagnostics and monitoring.

> **Note:** The Network Logs and Security Logs components require the device to be configured as a device owner. More details can be found in our [wiki page](#).

## Compatibility

Elastic Agent Android is designed to work with Android devices running **Android 7.0 (Nougat) and above**. 
The app is built using the latest Android SDK and follows best practices for compatibility and performance. 

To enroll the agent, you will need a Fleet server running **Elastic / Fleet 8.10.2 or later** (possibly earlier versions, but not tested).

## Quick Start

To get started with Elastic Agent Android, follow these steps:

### 1. Download the APK
Download the latest APK from the [Artifacts section](https://github.com/swiftbird07/elastic-agent-android/actions) of the GitHub Actions page. Choose the latest successful build and download the APK by scrolling down and clicking on the `elastic-agent-android.apk`.

### 2. Create a New Policy in Fleet
In your Fleet server, create a new policy using the "Custom Logs" integration (a "real" Android integration will be available in the future)
This policy will define which components of the Elastic Agent Android will be activated.

### 3. Configure the Policy
- Under "Paths", specify one path for each component you wish to activate. Examples include:
    - `android://self-log.warn` for warning level self logs.
    - `android://location.fine?minTimeMs=300000&minDistanceMeters=50` for fine location updates every 5 minutes or 50 meters.
    - `android://security-logs.all` for all security logs (device owner required).
    - `android://network-logs.all` for all network logs (device owner required).
- In "Advanced options" -> "Custom Configurations", add:
```yaml
  max_documents_per_request: 200
  put_interval: 1m
  checkin_interval: 1m
  use_backoff: true
  max_backoff_interval: 5m
  backoff_on_empty_buffer: false
  disable_on_low_battery: false
```
These settings control how documents are batched and sent to Elasticsearch, with options for backoff strategies.

### 4. Install the App
Install the downloaded APK on your target Android device.

### 5. Enroll the Agent
Open the app and tap on "Enroll Agent". Fill in the server URL, enrollment token, and hostname. You can also toggle SSL verification as needed. For mass enrollment, these fields can be autofilled using the clipboard or build configurations.

### 6. Verify the Enrollment
After enrolling, the agent should report as "Healthy" within a few seconds, and you should start seeing events in Elasticsearch based on the `put_interval` setting.

## Contributions and Feedback

We are open to feature requests, contributions, questions, and any feedback. If you're interested in contributing or have suggestions for improvement, please feel free to reach out or submit an issue/pull request on our GitHub repository. 
See [CONTRIBUTE.md](CONTRIBUTE.md) for more info about how to contribute, as well as for a general overview of the app's architecture.

 
---
_**Disclaimer:** This project is not affiliated with Elastic N.V. or their Elastic Agent offerings. It is an independent implementation created by the community for educational and experimental purposes.
The maintainers of this project are not responsible for any misuse or damage caused by the software. Use at your own risk. See license for more details.
The Elastic Agent logo is a registered trademark of Elastic N.V. and is used here for illustrative purposes only and does not imply any affiliation with or endorsement by Elastic N.V._
