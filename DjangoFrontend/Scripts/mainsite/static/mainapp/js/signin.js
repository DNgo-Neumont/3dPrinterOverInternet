document.getElementById("submit_button").addEventListener("click", callback)

function callback(){
    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;
    var body = {
        "user_name": username,
        "password": password
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
    
    request.open("POST", "/login/")
    request.setRequestHeader("Content-Type", "application/json");
    request.send(JSON.stringify(body))
}