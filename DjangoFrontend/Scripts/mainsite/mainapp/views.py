import traceback
from urllib.error import HTTPError
from django.shortcuts import render, redirect
from django.http.response import HttpResponse, HttpResponseRedirect, JsonResponse
from django.urls import reverse
from django.template import loader
from django.template import Template
from django.contrib.auth import logout
from rest_framework.decorators import api_view
from py_eureka_client import eureka_client
import json

# Create your views here.
@api_view(("GET",))
def index(request):
    print(request)
    if("auth_tokens" in request.session.keys()):
        print(request.session["auth_tokens"])
        authenticated = {
            "auth": True,
            "username": request.session["username"]
        }
    else:
        authenticated = {
            "auth": False
        }

    # print(kwargs)
    template = loader.get_template("index.html")
    # return HttpResponse(template.render(), content=authenticated)#, request)
    return render(request, "index.html", context=authenticated)

@api_view(("GET", "POST"))
def login_render(request, *args, **kwargs):
    print(request)
    if(request.method == "GET"):
        # template = loader.get_template("signin.html")
        print(request.session)
        if("auth" in request.session):
            print(request.session["auth"])
        if("username" in request.session):
            print(request.session["username"])


        return render(request, "signin.html")
    elif(request.method == "POST"):
        print(request.data)

        # body = dict(request.body)

        # print(body)

        postData = {
            "user_name": request.data["user_name"],
            "password": request.data["password"]
        }

        headerDef = {
            "Content-Type": "application/json"
        }

        try:

            res = eureka_client.do_service("USER-SERVICE", "/user/auth", method="POST", data=json.dumps(postData), headers=headerDef)

            resJson = json.loads(res)

            tokens = {}
            access_token = resJson["access_token"]
            refresh_token = resJson["refreshToken"]

            tokens["access_token"] = access_token
            tokens["refresh_token"] = refresh_token
        

            request.session["auth_tokens"] = tokens
            request.session["auth"] = True
            request.session["username"] = postData["user_name"]

        except Exception as exception:
            traceback.print_exception(Exception, exception, exception.__traceback__)
            
            responseData = {
                "message": "Unauthorized"
            }
            
            return JsonResponse(responseData)

        template = loader.get_template("index.html")
        # return HttpResponse(template.render(), tokens)
        responseData = {
            "message" : "ok"
        }
        return JsonResponse(responseData)#, kwargs=tokens)

@api_view(("GET",))
def logoutUser(request, *args, **kwargs):
    logout(request)
    return render(request, "index.html")

@api_view(("GET", "POST"))
def registerUser(request, *args, **kwargs):
    if(request.method == "GET"):
        return render(request, "register.html")
    else:
        # return render(request, "index.html")

        postData = {
            "userName": request.data["user_name"],
            "password": request.data["password"],
            "userEmail": request.data["email"],
            "roles": "ROLE_USER"
        }

        headerDef = {
            "Content-Type": "application/json"
        }
        try:
            res = eureka_client.do_service("USER-SERVICE", "/user/", method="POST", data=json.dumps(postData), headers=headerDef)
            resJson = json.loads(res)
            return JsonResponse(resJson)
        except Exception as exception:
            traceback.print_exception(Exception, exception, exception.__traceback__)
            
            response = {
                "message": "failed to sign up - duplicate username",
                "status": 400
            }

            return JsonResponse(response)
        
@api_view(("GET", "POST"))
def user_update(request, *args, **kwargs):
    try:

        headerDef = {
            "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
        }

        print(headerDef)

        res = eureka_client.do_service("USER-SERVICE", "/user/userLookup/" + kwargs["username"], method="GET", headers=headerDef)
        
        resJson = json.loads(res)

        formattedUser = resJson["user"]

        user = {
            # "username": request.session["username"],
            "username": formattedUser["userName"],
            "auth": request.session["auth"],
            "email": formattedUser["userEmail"]
        }

        print(user.get("username"))

        return render(request, "user.html", context=user)
    except HTTPError as httpError:
        if(httpError.code == 403 or httpError.code == 500):
            print(httpError.code)


            headerDef = {
                "Authorization": "Bearer " + request.session["auth_tokens"]["refresh_token"]
            }            
            print(headerDef)
            print("old tokens")
            print(request.session["auth_tokens"])

            print("doing refresh call")
            res = eureka_client.do_service("USER-SERVICE", "/user/refreshAuth", method="GET", headers=headerDef)

            print("successful refresh call")
            authJson = json.loads(res)

            print("json response: ", authJson)
            tokens =  {
                "access_token": authJson["access_token"],
                #FOR SOME REASON THIS ONE COMES IN WITH A UNDERSCORE.
                "refresh_token": authJson["refresh_token"]
            }

            request.session["auth_tokens"] = None

            print(request.session["auth_tokens"])
            request.session["auth_tokens"] = tokens
            print("new tokens: ")
            print(request.session["auth_tokens"])

            headerDef = {
                "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
            }

            res = eureka_client.do_service("USER-SERVICE", "/user/userLookup/" + kwargs["username"], method="GET", headers=headerDef)
            resJson = json.loads(res)
            formattedUser = resJson["user"]

            user = {
                # "username": request.session["username"],
                "username": formattedUser["userName"],
                "auth": request.session["auth"],
                "email": formattedUser["userEmail"]
            }
            return render(request, "user.html", context=user)
    except Exception as exception:
        traceback.print_exception(Exception, exception, exception.__traceback__)
        
        # response = {
        #     "message": "failed to sign up - duplicate username",
        #     "status": 400
        # }

        return render(request, "signin.html")

