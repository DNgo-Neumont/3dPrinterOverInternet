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
import logging

from django.core.wsgi import get_wsgi_application
from socketservercapstone.sioinit import sio as baseSocketServer

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'socketservercapstone.settings')

application = get_wsgi_application()

sio = baseSocketServer
app = socketio.WSGIApp(sio, application)

authSecret = os.environ["JWT_SECRET"]

logger = logging.getLogger(__name__)

@sio.on("connect", namespace="/")
def authenticate(sid, connectionDetails,  data):
    
    # logger.debug("connection details ", str(connectionDetails))
    # logger.debug("Client with session ID " + sid + " connected")

    # logger.debug("Incoming data: " + str(data))
    print("connection details: ",connectionDetails)
    print("data: ",data)
    print("sid: ",sid)
    token = data["token"]

    # Change to a file at some point - or have it handled in 
    # settings.py
    validIssuers = ["https://simplprint.azurewebsites.net/user/auth", "https://simplprint3d.com/user/auth", "https://simplprint3d.com/user/piAuth", "https://simplprint.azurewebsites.net/user/piAuth"]

    try:
        decodedToken = jwt.decode(token, key=authSecret, algorithms=["HS256"])
        print(decodedToken)

        # validIssuer = False

        # commented out for local docker testing - do not deploy
        # without uncommenting
        # for issuer in validIssuers:
        #     if((issuer in decodedToken.get("iss"))):# or validIssuers[1] in decodedToken.get("iss"))):
        #         validIssuer = True
        
        # if(not validIssuer):
        #     sio.disconnect(sid=sid)
        #     # logger.error("Invalid token issuer")
        #     raise Exception("Invalid issuer for token, refusing connection")
        
        sio.enter_room(sid, decodedToken.get("sub"))

    except Exception as exception:
        sio.disconnect(sid=sid)
        # logger.error(traceback.format_exception(Exception, exception, exception.__traceback__))
        traceback.print_exception(Exception, exception, exception.__traceback__)
        print("token is invalid, refusing connection")

        #@

@sio.on("status")
def onPrint( sid, data):
    print(data)
    # logger.debug("Status response recieved: "  + str(data))
    #Sends data back to our frontend, which will parse out the current status and stick it in a database or something
    #be sure to include the username in the data packet to determine origin
    sio.emit("status-handler", data, "frontend-server")

eurekaHost = os.environ["EUREKA_HOST"]
eurekaPort = os.environ["EUREKA_PORT"]

eureka_client.init(f"http://{eurekaHost}:{eurekaPort}", app_name="django-socket-server",instance_port=8080)

eventlet.wsgi.server(eventlet.listen(("", 8080)), app)