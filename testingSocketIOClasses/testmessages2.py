from datetime import datetime, tzinfo
import socketio
import pytz

sio = socketio.Client()


@sio.on("connect")
def connectionMessage():
    print("connected to server")
    response = {
        "groupName":"test",
        "userID":3
    }

    sio.emit("connectToRoom",response)

@sio.on("disconnect")
def disconnectMessage():
    print("disconnected from server")

@sio.on('print')
def messsageRecieved(data):
    print(data)

sio.connect("http://localhost:80/socket", wait_timeout=10, auth={"token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0VXNlcjA0Iiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlzcyI6Imh0dHBzOi8vc2ltcGxwcmludC5henVyZXdlYnNpdGVzLm5ldC91c2VyL2F1dGgiLCJleHAiOjE2Njc5NDM1MDF9.3ZKjckaSxWXoKAzLdzrWO7wgavrRiBgqwkwZReYAu0k"})


@sio.on("room-response")
def roomResponseReceived(data):
    print(data)

while(True):
    message = input("Enter a message: ")
    response = {
        "groupName": "test",
        "message":{
            "userID":3,
            "name":"David",
            "content": message,
            "postTime": str(datetime.now(tz=pytz.UTC))
        }
    }
    sio.emit("status", response)
