from datetime import datetime
import os
import traceback
from django.http.response import JsonResponse
from django.shortcuts import render
from rest_framework.decorators import api_view
from socketservercapstone.sioinit import sio as baseSocketServer
# from socketservercapstone.wsgi import sio, authSecret
import jwt
# Create your views here.

authSecret = os.environ["JWT_SECRET"]
sio = baseSocketServer

# Issuer check ends up screwing up half our front end functions so it's out.
validIssuers = ["https://simplprint.azurewebsites.net/user/auth", "https://simplprint3d.com/user/auth"]

@api_view(("POST",))
def queuePrint(request, *args, **kwargs):
    
    requestToken = request.META["HTTP_AUTHORIZATION"]
    
    requestToken = str(requestToken).replace("Bearer", "").strip()

    print("Incoming req token")
    print(requestToken)


    print(request.data)

    requestData = dict(request.data)

    print(requestData)

    dataToEmit = {
        "file": requestData.get("file"),
        "printer": requestData.get("printer")
    }


    try:
        decodedToken = jwt.decode(requestToken, key=authSecret, algorithms=["HS256"])

        print("Decoded token: ")
        print(decodedToken)

        # Remember to uncomment once you push it
        # if(not(validIssuers[0] in decodedToken.get("iss") or validIssuers[1] in decodedToken.get("iss"))):
        #     raise Exception("Invalid issuer for token, refusing connection")

    except Exception as exception:
        traceback.print_exception(Exception, exception, exception.__traceback__)
        #return proper response here
        response = {
            "message":"Auth token provided invalid",
            "timestamp": datetime.now()
        }
        return JsonResponse(response, status=403)



    username = decodedToken.get("sub")

    sio.emit("queue", data=dataToEmit, to=username )

    response = {
        "message":"File queued for print successfully",
        "timestamp": datetime.now()
    }

    return JsonResponse(response, status=200)


@api_view(("GET",))
def requestStatus(request, *args, **kwargs):

    requestToken = request.META["HTTP_AUTHORIZATION"]
    
    requestToken = str(requestToken).replace("Bearer", "").strip()

    print("Incoming req token")
    print(requestToken)


    print(request.data)

    requestData = dict(request.data)

    print(requestData)

    try:
        decodedToken = jwt.decode(requestToken, key=authSecret, algorithms=["HS256"])

        print("Decoded token: ")
        print(decodedToken)

        # Make sure to uncomment when pushing
        # if(not(validIssuers[0] in decodedToken.get("iss") or validIssuers[1] in decodedToken.get("iss"))):
        #     raise Exception("Invalid issuer for token, refusing connection")

    except Exception as exception:
        traceback.print_exception(Exception, exception, exception.__traceback__)
        #return proper response here
        response = {
            "message":"Auth token provided invalid",
            "timestamp": datetime.now()
        }
        return JsonResponse(response, status=403)
    
    username = decodedToken.get("sub")

    sio.emit("request-update", None, username)

    response = {
        "message":"Update request sent",
        "timestamp": datetime.now()
    }

    return JsonResponse(response, status=200)
