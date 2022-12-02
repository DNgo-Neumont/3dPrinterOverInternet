
import json
import traceback
from urllib.error import HTTPError
from py_eureka_client import eureka_client
import socketio
import os
from django.conf import settings
import time

os.environ.setdefault("EUREKA_HOST", "localhost")
os.environ.setdefault("EUREKA_PORT", "8761")

eurekaHost = os.environ["EUREKA_HOST"]
eurekaPort = os.environ["EUREKA_PORT"]

eureka_client.init(f"http://{eurekaHost}:{eurekaPort}", app_name="django-frontend",instance_port=8000)

# set up socketIO listener here for usage to serialize user DB info - in terms of user consumers and connected printers


hasToken = False

serverAuthTokens = {}

while(not hasToken):

    print("retrieving token from userAPI for usage")

    time.sleep(2)

    # print("WARNING - REMOVE DEBUG PRINT HERE BEFORE PROPER DEPLOYMENT")
    # print("Secret key comes in as: ")
    # print(settings.SECRET_KEY)

    postData = {
        "userName": "frontend-server",
        "password": settings.SECRET_KEY,
        "userEmail": "dngo@student.neumont.edu",
        "roles": "ROLE_USER"
    }

    headerDef = {
        "Content-Type": "application/json"
    }

    try:
        res = eureka_client.do_service("USER-SERVICE", "/user/", method="POST", data=json.dumps(postData), headers=headerDef)
        resJson = json.loads(res)
        print("User for frontend service created: details printed")
        print(resJson)
    except HTTPError as httpExcept:
        print(httpExcept.code)
        if(httpExcept.code == 503):
            print("User service unresponsive - retrying")
            continue
        elif(httpExcept.code == 400):
            print("already made a useraccount - continuing with loop.")
    except Exception as exception:
        traceback.print_exception(Exception, exception, exception.__traceback__)
        print("issue creating user account - retrying")
        continue
    
    authPostData = {
        "user_name": "frontend-server",
        "password": settings.SECRET_KEY
    }

    try:
        resAuth = eureka_client.do_service("USER-SERVICE", "/user/piAuth", method="POST", data=json.dumps(authPostData), headers=headerDef)

        resAuthTokens = json.loads(resAuth)
        serverAuthTokens = resAuthTokens
    except HTTPError as httpExcept:
        print(httpExcept.code)
        if(httpExcept.code == 503):
            print("User service unresponsive - retrying")
            continue
        elif(httpExcept.code == 400):
            print("bad request body sent - you should never see this - check your code.")
            raise
    except Exception as exception:
        traceback.print_exception(Exception, exception, exception.__traceback__)
        print("issue getting auth tokens - retrying")
        continue

    hasToken = True

