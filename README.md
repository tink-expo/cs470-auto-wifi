# Android App for automatic Wifi connection using OCR
>This is a team 12 repository for CS470 final project.

## Application Overview
When using the provided network in places like cafe, people have to search for the matching WIFI network ID in the available WIFI list and type the PW manually.
**This Android application will enable user to connect to WIFI automatically by just simply scanning the picture that contains WIFI ID and PW**

<img src="1.jpg" width="450px" height="300px"></img>


## Process Pipeline
1. [Input] Picture that contains WIFI ID and PW. The picture can contain letters other than the ID and PW, and the letters can be hand-written.
2. Detect the words and their positions using OCR pretrained model. 
3. From the detected information in step 2, extract WIFI ID and PW.
4. Using Android network API, connect to WIFI automatically using retrieved ID and PW.

## Considerations for Implementation
### Evaluation
Though we use pretrained model for OCR, we have to post process the ID and PW information from the OCR model output. We will evaluate out application by measuring the success rate of "Extracting both correct ID and PW from arbitrary WIFI information picture".

### Input data
Though we use pretrained model, we need input data for making a good post processing strategy and evaluation.
We will first collect some real WIFI information picture data by Hand collecting and crawl from Google image search.
Since the images we require are very specific, the amount of data that can be collected by above ways might not be large. However, the reason we need input data is not for training but for evaluation and deciding post processing strategy. If the collected data is too small even for above objectives, we will search for other ways to enlarge the data. (e.g. generation based on collected real data)

### OCR model
We will modularize here, and first use OCR cloud API. 
This will require network connection, so we will try applying an on-device model if time allows. 
Model output will be collection of 
{Detected characters, Bounding box position and size}s 

### Post processing for ID and PW extraction
We will first try deterministic way for this, using approaches listed below. If it is too unsuccessful, we should search for other approaches.
- Word position and size. (Centered, Big, SSID is usually followed by Password, etc)
- Field tag. (ID, PW, etc)
- Match with retrieved available Wi-fi SSIDs.
