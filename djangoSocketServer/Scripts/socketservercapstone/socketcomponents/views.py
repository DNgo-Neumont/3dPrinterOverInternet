from datetime import datetime
import traceback
from django.http.response import JsonResponse
from django.shortcuts import render
from rest_framework.decorators import api_view
from socketservercapstone.wsgi import sio, authSecret
import jwt
# Create your views here.

validIssuers = ["https://simplprint.azurewebsites.net/user/auth", "https://simplprint3d.com/user/auth"]

@api_view(("POST",))
def queuePrint(request, *args, **kwargs):
    
    requestToken = request.auth
    
    print(request.data)

    requestData = dict(request.data)

    print(requestData)

    dataToEmit = {
        "file": requestData.get("file"),
        "printer": requestData.get("printer")
    }


    try:
        decodedToken = jwt.decode(requestToken, authSecret, ["HS256"])

        print(decodedToken)

        if(not(validIssuers[0] in decodedToken.get("iss") or validIssuers[1] in decodedToken.get("iss"))):
            raise Exception("Invalid issuer for token, refusing connection")

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


