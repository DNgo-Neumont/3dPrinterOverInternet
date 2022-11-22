document.getElementById("submit_button").addEventListener("click", callback)

var ogUsername = document.getElementById("username_display").innerHTML

function callback(){
    var username = document.getElementById("change_username").value;
    var email = document.getElementById("change_email").value;
    var password = document.getElementById("change_password").value;
    var verifypass = document.getElementById("confirm_password").value;

    ogUsername = ogUsername.replace("Username:", "");
    console.log(ogUsername);
    ogUsername = ogUsername.trim();
    console.log(ogUsername);

    if(password === verifypass){
        document.getElementById("error_password").style.display = "none";
        requestUsername = ogUsername;
        var body = {}
        if(username){
            body["change_username"] = username;
            requestUsername = username;
        }

        if(email){
            body["change_email"] = email;
        }

        if(password){
            body["change_password"] = password;
        }

        var request = new XMLHttpRequest();
        
        request.onreadystatechange = function(){
            // console.log(this.getAllResponseHeaders())    
            // document.location.assign(request.getResponseHeader("Location"))
    
            if(this.readyState == 4 && this.status == 200){
                var responseTextAsJson = JSON.parse(request.responseText)
        
                if(responseTextAsJson.message.includes("Unauthorized")){
                    document.getElementById("error_message").style.display = "block"
                    console.log("failed to login")
                }else{
                    document.getElementById("error_message").style.display = "none"
                    document.location.assign("/")
                }
            }
        }
        
        request.open("POST", "/" + ogUsername + "/")
        request.setRequestHeader("Content-Type", "application/json");
        request.send(JSON.stringify(body))
    }else{
        document.getElementById("error_password").style.display = "block";
    }

}