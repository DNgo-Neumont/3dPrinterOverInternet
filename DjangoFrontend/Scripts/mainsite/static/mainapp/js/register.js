document.getElementById("submit_button").addEventListener("click", callback)

function callback(){
    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;
    var email = document.getElementById("email").value;
    var body = {
        "user_name": username,
        "password": password,
        "email": email,
    }
    
    var request = new XMLHttpRequest();
    
    request.onreadystatechange = function(){
        // console.log(this.getAllResponseHeaders())    
        // document.location.assign(request.getResponseHeader("Location"))
        if(this.readyState == 4 && this.status == 200){
            console.log(this.responseText)
            
            var responseTextAsJson = JSON.parse(request.responseText)
            console.log(responseTextAsJson);
            if(responseTextAsJson.message.includes("failed")){
                console.log("failed to register")
                document.getElementById("error_message").style.display = "block";
            }else{
                document.getElementById("error_message").style.display = "none";
                document.location.assign("/login/")
            }
        }

    }
    
    request.open("POST", "/register/")
    request.setRequestHeader("Content-Type", "application/json");
    request.send(JSON.stringify(body))
}