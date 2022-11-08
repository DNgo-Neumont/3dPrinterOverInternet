"""
WSGI config for socketservercapstone project.

It exposes the WSGI callable as a module-level variable named ``application``.

For more information on this file, see
https://docs.djangoproject.com/en/4.1/howto/deployment/wsgi/
"""

import os
import traceback
import socketio
import eventlet
import eventlet.wsgi
import jwt
import py_eureka_client.eureka_client as eureka_client

from django.core.wsgi import get_wsgi_application

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'socketservercapstone.settings')

application = get_wsgi_application()

sio = socketio.Server()
app = socketio.WSGIApp(sio, application)

authSecret = os.environ["JWT_SECRET"]

@sio.on("connect")
def authenticate(sid, connectionDetails, data):
    print("connection details ", connectionDetails)
    print("data ",data)
    token = data["token"]

    validIssuers = ["https://simplprint.azurewebsites.net/user/auth", "https://simplprint3d.com/user/auth"]

    try:
        decodedToken = jwt.decode(token, key=authSecret, algorithms=["HS256"])
        print(decodedToken)

        if(not(validIssuers[0] in decodedToken.get("iss") or validIssuers[1] in decodedToken.get("iss"))):
            sio.disconnect(sid=sid)
            raise Exception("Invalid issuer for token, refusing connection")
    except Exception as exception:
        sio.disconnect(sid=sid)
        traceback.print_exception(Exception, exception, exception.__traceback__)
        print("token is invalid, refusing connection")

        #@

@sio.on("status")
def onPrint( sid, data):
    print(data)
    #Sends data back to our frontend, which will parse out the current status and stick it in a database or something
    #be sure to include the username in the data packet to determine origin
    sio.emit("status-handler", data, "frontend-status")

eurekaHost = os.environ["EUREKA_HOST"]
eurekaPort = os.environ["EUREKA_PORT"]

eureka_client.init(f"http://{eurekaHost}:{eurekaPort}", app_name="django-socket-server",instance_port=8080)

eventlet.wsgi.server(eventlet.listen(("", 8080)), app)