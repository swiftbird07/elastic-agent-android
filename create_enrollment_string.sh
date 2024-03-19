#!/bin/bash

# Define file to hold persistent values
PERSISTENT_VALUES_FILE="enrollment_values.env"
CREDENTIALS_FILE="credentials.env"

# Function to save enrollment values
save_values() {
    echo "serverUrl='$serverUrl'" > "$PERSISTENT_VALUES_FILE"
    echo "hostname='$hostname'" >> "$PERSISTENT_VALUES_FILE"
    echo "tags='$tags'" >> "$PERSISTENT_VALUES_FILE"
    echo "verifyServerCert=$verifyServerCert" >> "$PERSISTENT_VALUES_FILE"
    echo "pinRootCert=$pinRootCert" >> "$PERSISTENT_VALUES_FILE"
}

# Function to save credentials
save_credentials() {
    echo "username='$username'" > "$CREDENTIALS_FILE"
    echo "password='$password'" >> "$CREDENTIALS_FILE"
}

# Function to fetch JWT token
fetch_token() {
    source "$PERSISTENT_VALUES_FILE"
    if [ -f "$CREDENTIALS_FILE" ]; then
        source "$CREDENTIALS_FILE"
    else
        echo "Username:"
        read username
        echo "Password:"
        read -s password
        save_credentials
    fi

    token=$(curl -u "$username:$password" -k -X POST "$serverUrl:55000/security/user/authenticate" | jq -r '.data.token')
    if [ "$token" == "null" ]; then
        echo "Failed to fetch token. Please check your credentials or network connection."
        exit 1
    fi
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
                    --arg tags "$tags" \
                    --argjson verifyServerCert "$verifyServerCert" \
                    --argjson pinRootCert "$pinRootCert" \
                    '{serverUrl: $serverUrl, token: $token, hostname: $hostname, tags: $tags, verifyServerCert: $verifyServerCert, pinRootCert: $pinRootCert}')

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

if [[ "$mode" == "refresh" ]]; then
    # Fetch new token using saved credentials
    fetch_token
    source "$PERSISTENT_VALUES_FILE" # Load existing enrollment values to keep them unchanged
else
    # Fresh input or updating all values
    echo "Enter server URL:"
    read serverUrl
    echo "Enter hostname (you can use %DEVICENAME% as a placeholder):"
    read hostname
    echo "Enter tags:"
    read tags
    echo "Verify server certificate? (true/false):"
    read verifyServerCert
    echo "Pin root certificate after enrollment? (true/false):"
    read pinRootCert
    # Fetch token for the first time
    fetch_token
    # Save the entered enrollment values
    save_values
fi

# Encode and print the final JSON string
encode_json
