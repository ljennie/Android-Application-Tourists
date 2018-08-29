var functions = require('firebase-functions');
var admin = require('firebase-admin');

admin.initializeApp(functions.config().firebase);

//listen to change on database
exports.sendNotification = functions.database.ref('/events/{pushId}')//'/events/{pushId}'
       .onWrite((change, context) => {//对于onwrite事件进行监听，change那一行和change的context参数进行传入

           // Grab the current value of what was written to the Realtime Database.
           var eventSnapshot = change.after.val();//将change完后的value存在eventSnapshot
                                                  //database修改的状态
           var topic = "android";//发送message的topic
           var payload = {//payload就是发送notification
               data: {
                     id : eventSnapshot.id,
                     title : eventSnapshot.title,
                     description : eventSnapshot.description,
                     address : eventSnapshot.address,
                     imgUri : eventSnapshot.imgUri
               }
           };

       // Send a message to devices subscribed to the provided topic.
       return admin.messaging().sendToTopic(topic, payload)
           .then(function (response) {
               // See the MessagingTopicResponse reference documentation for the
               // contents of response.
               console.log("Successfully sent message:", response);
               return -1;
           })
           .catch(function (error) {
               console.log("Error sending message:", error);
           });
       })



//const functions = require('firebase-functions');
//
//// // Create and Deploy Your First Cloud Functions
//// // https://firebase.google.com/docs/functions/write-firebase-functions
////
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
//  console.log("Hello world lalalalalala");
// });
