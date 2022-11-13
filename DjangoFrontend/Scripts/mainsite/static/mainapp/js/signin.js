document.getElementById("submit_button").addEventListener("click", callback)

function callback(){
    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;
    var body = {
        "user_name": username,
        "password": password
    }
    
    var request = new XMLHttpRequest();
    
    // request.onreadystatechange() = function(){
    
    // }
    
    request.open("POST", "/login/")
    request.setRequestHeader("Content-Type", "application/json");
    request.send(JSON.stringify(body))
}