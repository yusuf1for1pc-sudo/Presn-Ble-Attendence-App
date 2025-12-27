// In ble/BleConstants.kt

package com.example.bleattendance.ble

import java.util.UUID

// Custom UUIDs for our attendance app - using a custom base UUID
// This avoids conflicts with reserved Bluetooth UUIDs
val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789ABC")

// A unique identifier for our writable characteristic (the "mailbox").
val STUDENT_CHARACTERISTIC_UUID: UUID = UUID.fromString("87654321-4321-4321-4321-CBA987654321")

// A unique identifier for sending responses back to students (queue status, etc.)
val RESPONSE_CHARACTERISTIC_UUID: UUID = UUID.fromString("11111111-2222-3333-4444-555555555555")
