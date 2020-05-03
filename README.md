# Meter Image Capturing
[![version](https://img.shields.io/badge/version-2.0.2-success.svg)](https://semver.org)

MiC is a mobile application designed for the Android operating system written in the Java programming language.  An iOS version does not exist at this time.  The main use case for the app is to use OCR (optical character recognition) to extract the progressive dollar values from images of slot machines.  The objective here is to save the casino employees time so they don't have to manually log all progressive values for each slot machine they monitor.  

## OCR ##

The main backend logic behind the OCR comes from Google's ML Kit SDK.  The SDK provides a number of machine learning APIs; MiC uses the text recognition API originally optimized for recognizing text in documents.  While the SDK provides support for text recogntion both on-device and by making a call to the cloud, the cloud model is the one used in the application.  This means that some sort of network connection (Wifi or Data) is required to make a successfull call.  Note: progressive values can still be inputted manually through the keyboard and submitted to the database without a network connection.  In this case the values will be stored in a queue until the next time the device connects to the internet, at which point they'll be pushed to the database.  The quality of the image the user captures correlates to the accuracy of the progressive text detected.  For best results ensure that the camera is properly focused and is not blurry.  

Note the current version of ML Kit used by the application is 23.0.0.  Also, MiC requires at least Android API level 23, or version 6.0 (Marshmallow).  

## Database ##

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

formUploads
 * uploadFormData

admins
 * 

Note the current version of cloud firestore used by the application is 21.0.0.  

### Indexes ###
There is a composite index on the scans collection with uid ascending and timestamp descending.  

### Rules ###
The following security rules are in place to ensure data stored in our database is secure.  

```
rules_version = '2';
service cloud.firestore {
	match /databases/{database}/documents {    
		match /scans/{documentId} {
			allow read, update, delete: if request.auth.uid == resource.data.uid;
			allow create: if request.auth.uid != null;
		}
		match /users/{document=**} {
			allow read, write: if request.auth.uid != null;
		}
		match /formUploads/{document=**} {
			allow read, write: if request.auth.uid != null;
		}
	}
}
```

## Authentication ##

MiC uses the Firebase authentication SDK to handle user registration and sign in.  Passwords are encrypted and completely handled by Google's API.  The developers of MiC have no access to a user's password, only the email they are using as a username.  Note that this information is not stored in the Cloud Firestore discussed above.  Completely separate processes are used for authentication and database management.  

Note the current version of the google-services api used by the application is 4.3.2.

## Dependencies ##

The following dependencies are necessary in the app level gradle file

```
implementation fileTree(dir: 'libs', include: ['*.jar'])
implementation 'androidx.appcompat:appcompat:1.1.0'
implementation 'androidx.legacy:legacy-support-v4:1.0.0'
implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
testImplementation 'junit:junit:4.12'
androidTestImplementation 'androidx.test:runner:1.2.0'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.2.0'

implementation 'com.google.firebase:firebase-ml-vision:24.0.1'
implementation 'com.google.firebase:firebase-auth:19.2.0'
implementation 'com.google.firebase:firebase-firestore:21.3.1'
implementation 'com.firebaseui:firebase-ui-auth:6.0.2'

implementation 'com.google.android.material:material:1.0.0'
implementation 'androidx.recyclerview:recyclerview:1.1.0'
implementation 'androidx.cardview:cardview:1.0.0'

implementation 'androidx.core:core:1.1.0'
implementation 'androidx.preference:preference:1.1.0'
implementation 'androidx.exifinterface:exifinterface:1.1.0'
```

The current gradle and google-services repositories are:

```
dependencies {
	classpath 'com.android.tools.build:gradle:3.5.3'
	classpath 'com.google.gms:google-services:4.3.3'
}
```

The google-services.json file should be placed in the app level directory of the project.

## Screenshots ##

<img src="https://raw.githubusercontent.com/alexanderjpowell/meter-image-capturing/master/docs/main_activity.png" width="250">

<img src="https://raw.githubusercontent.com/alexanderjpowell/meter-image-capturing/master/docs/report_data_activity.png" width="250">

<img src="https://raw.githubusercontent.com/alexanderjpowell/meter-image-capturing/master/docs/nav_bar.png" width="250">

## Permissions ##

The MiC app requires the following permissions to be granted by the user to properly function:
 * INTERNET
 * RECORD_AUDIO
 * VIBRATE
 * CAMERA
 * READ_EXTERNAL_STORAGE
 * WRITE_EXTERNAL_STORAGE

## Admin Permissions ##

The MiC application supports granting admin access to another email address.  This new email will have full read access to all casinos who register it as an admin.  To add an admin account to the platform perform the following:

1. Create the account in the Firebase console under the Authentication tab.  Do not use an email that already belongs to another casino.
2. Write a new document in the database per the following structure.  This will all take place in the ```admins``` collection.
```
admins (collection)
	adminexample@email.com (document)
		adminUID: 'bgsmVEsZV6PLk57lOVW1KIfr0xe2'
		casinos (collection)
			'1kyN8HCbC6gfZY8nNIYB1HjqRnH3' (document)
				casinoName: Joe's Casino
```












