# Elastic Agent Android

[![Android CI](https://github.com/swiftbird07/elastic-agent-android/actions/workflows/android.yml/badge.svg)](https://github.com/swiftbird07/elastic-agent-android/actions/workflows/android.yml)

Inofficial Elastic Agent for Android. This project is not affiliated with Elastic.

## Planned Features

- [x] Enroll Agent to Elastic Fleet Server
- [x] Manage Agent via Kibana's Fleet Management
- [x] Send events to Elastic Stack depending on the current policy
  - [x]  Basic system information
  - [x]  Various android system logs (requires device admin permission)
  - [x]  Location information
  - [ ]  Battery information
  - [x]  Network information
    - [ ]   Network usage information
    - [ ]   Detailed HTTP request information
    - [x]   Detailed DNS request information
    - [x]   Flow data
- [ ] Support for custom event types
