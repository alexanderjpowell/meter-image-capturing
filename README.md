# Meter Image Capturing

MiC is a mobile application designed for the Android operating system written in the Java programming language.  An iOS version does not exist at this time.  The main use case for the app is to use OCR (optical character recognition) to extract the progressive dollar values from images of slot machines.  The objective here is to save the casino employees time so they don't have to manually log all progressive values for each slot machine they monitor.  

##OCR##

The main backend logic behind the OCR comes from Google's ML Kit SDK.  The SDK provides a number of machine learning APIs; MiC uses the text recognition API originally optimized for recognizing text in documents.  While the SDK provides support for text recogntion both on-device and by making a call to the cloud, the cloud model is the one used in the application.  This means that some sort of network connection (Wifi or Data) is required to make a successfull call.  Note: progressive values can still be inputted manually through the keyboard and submitted to the database without a network connection.  In this case the values will be stored in a queue until the next time the device connects to the internet, at which point they'll be pushed to the database.  The quality of the image the user captures correlates to the accuracy of the progressive text detected.  For best results ensure that the camera is properly focused and is not blurry.  

Note the current version of ML Kit used by the application is 19.0.2.  Also, MiC requires at least Android API level 23, or version 6.0 (Marshmallow).  

##Database##

The backend database is powered by Cloud Firestore which is a NoSQL cloud database provided by Google Firebase.  It is document based and designed to be fast and scalable.  In the Firestore model, documents are stored in collections and can contain fields as well as other collections.  The current schema contains two collections: scans and users.  The scans collection contains documents with details about each image capture, and the users collection stores data specific to each user account.  

scans
 * email
 * machine_id
 * notes
 * progressive1
 * progressive2
 * progressive3
 * progressive4
 * progressive5
 * progressive6
 * timestamp
 * uid
 * userName
users
 * displayNames
 * sortProgressives

###Indexes### - There is a composite index on the scans collection with uid ascending and timestamp descending.  

###Rules###
The following security rules are in place to ensure data stored in our database is secure.  

```
service cloud.firestore {
    match /databases/{database}/documents {
        match /{document=**} {
            allow read, write: if request.auth.uid != null;
        }
    }
}
```

##Authentication##



