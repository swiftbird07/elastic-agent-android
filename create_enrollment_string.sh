#!/bin/bash

# Define file to hold persistent values
PERSISTENT_VALUES_FILE="enrollment_values.env"

# Function to save enrollment values
save_values() {
    echo "serverUrl='$serverUrl'" > "$PERSISTENT_VALUES_FILE"
    echo "hostname='$hostname'" >> "$PERSISTENT_VALUES_FILE"
    echo "certificate='$certificate'" >> "$PERSISTENT_VALUES_FILE"
    echo "verifyServerCert=$verifyServerCert" >> "$PERSISTENT_VALUES_FILE"
    echo "pinRootCert=$pinRootCert" >> "$PERSISTENT_VALUES_FILE"
}


# Encode and print the JSON string
encode_json() {
    # Notify user about %DEVICENAME% placeholder
    echo "Note: You can use %DEVICENAME% as a placeholder for the device's name."

    # Create JSON string
    json_string=$(jq -n \
                    --arg serverUrl "$serverUrl" \
                    --arg token "$token" \
                    --arg hostname "$hostname" \
                    --arg certificate "$certificate" \
                    --argjson verifyServerCert "$verifyServerCert" \
                    --argjson pinRootCert "$pinRootCert" \
                    '{serverUrl: $serverUrl, token: $token, hostname: $hostname, certificate: $certificate, verifyServerCert: $verifyServerCert, pinRootCert: $pinRootCert}')

    # Encode to Base64
    echo "Encoded Enrollment String:"
    echo $(echo "$json_string" | base64 | tr -d "\n")
    echo "ENROLLMENT_STRING=$(echo "$json_string" | base64 | tr -d "\n")" > .env

}

# Check for jq and curl
if ! command -v jq &> /dev/null || ! command -v curl &> /dev/null
then
    echo "jq and curl are required to run this script. Please install them."
    exit
fi

# Main script starts here
mode="$1"

# Fresh input or updating all values
echo "Enter fleet URL:"
read serverUrl
echo "Enter token:"
read token
echo "Enter hostname (you can use %DEVICENAME% as a placeholder):"
read hostname
echo "Enter certificate:"
read certificate
echo "Verify server certificate? (true/false):"
read verifyServerCert
echo "Pin root certificate after enrollment? (true/false):"
read pinRootCert

# Save the entered enrollment values
save_values


# Encode and print the final JSON string
encode_json
