# Android App for automatic Wifi connection using OCR
>This is a team 12 repository for CS470 final project.

<br></br>

## Application Overview
When using the provided network in places like cafe, people have to search for the matching wifi SSID in the available Wifi list and type the password manually. This Android application will enable user to connect to wifi automatically by just simply scanning the picture that contains wifi SSID and password.

<br></br>

## Process Pipeline
1. Input: picture that contains wifi SSID and password taken by user.
2. Text detection (description string, bounding boxes) using Google AI Cloud Vision API. 
3. Output: extracted wifi SSID and password from the detection result, which is required to connect to wifi.
4. Using Android network API, connect to WIFI automatically using retrieved ID and PW.

<br></br>

## Use Flow
<img src="./image_readme/Use Flow.png"></img>

<br></br>
## Project document:
https://docs.google.com/document/d/1rUMfPKWnKN4waLV1V4iEL9Xn7siuPR_zNOVPaEmiBW4
