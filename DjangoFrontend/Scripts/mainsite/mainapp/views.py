import socketio
import traceback
from urllib.error import HTTPError
from django.shortcuts import render, redirect
from django.http.response import HttpResponse, HttpResponseRedirect, JsonResponse
from django.urls import reverse
from django.template import loader
from django.template import Template
from django.core.exceptions import ObjectDoesNotExist
from django.contrib.auth import logout
from rest_framework.decorators import api_view, APIView
from rest_framework.parsers import MultiPartParser, FormParser
import mainapp.models
from mainapp.__init__ import serverAuthTokens
from py_eureka_client import eureka_client
import json



sio = socketio.Client()

@sio.on("status-handler")
def storeUpdate(data):
    print("hit store update")
    print(data)
    # serialize and update data for user here
    username = data["user"]
    # this is a list coming in of all connected printers to a consumer - have to pull out each and serialize to a dict
    status = data["status"]

    # if(mainapp.models.User.objects.get(username=username) != None):
    try:
        userToUpdate = mainapp.models.User.objects.get(username=username)
        # userSerializer = mainapp.models.UserSerializer(userToUpdate)
        # printers = userSerializer.data["consumers"]
        printers = []

        for printer in status:
            printerStatus = {
                "username": username,
                "printer_name": printer["printer-name"],
                "printer_progress": printer["printer-progress"]
            }
            printers.append(printerStatus)

        userToUpdate.consumers = printers

        userToUpdate.save()

        serializer = mainapp.models.UserSerializer(userToUpdate)
        print(serializer.data)
    except Exception as exception:
        traceback.print_exception(Exception, exception, exception.__traceback__)

        printers = []
        for printer in status:
            printerStatus = {
                "username": username,
                "printer_name": printer["printer-name"],
                "printer_progress": printer["printer-progress"]
            }

        printers.append(printerStatus)
        userToSave = mainapp.models.User(username=username, consumers=printers)
        userToSave.save()
        serializer = mainapp.models.UserSerializer(userToSave)
        print(serializer.data)




    # listOfConsumers = []

    # consumer = {
    #     "username": username,
    #     "status": printers
    #     # "_id": ObjectId()
    # }

    # listOfConsumers.append(consumer)
    # consumers are redundant for our purposes and end up muddying the serialized data - then it's more legwork for me to deserialize it all for use.






@sio.on("connect")
def connected():
    print("successful connection to socketio server")

@sio.on("disconnect")
def disconnected():
    print("Lost connection to server - consider checking sent auth tokens")

sio.connect("http://socket-service:8080", auth={"token":serverAuthTokens["access_token"]})


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
    # template = loader.get_template("index.html")
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

            print("response json(login): ", resJson)

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
    if(request.method == "GET"):
        try:

            headerDef = {
                "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
            }

            # print(headerDef)

            res = eureka_client.do_service("USER-SERVICE", "/user/userLookup/" + kwargs["username"], method="GET", headers=headerDef)
            
            resJson = json.loads(res)

            formattedUser = resJson["user"]

            request.session["user_id"] = formattedUser["userId"]

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
                try:
                    print(httpError.code)


                    headerDef = {
                        "Authorization": "Bearer " + request.session["auth_tokens"]["refresh_token"]
                    }            
                    # print(headerDef)
                    # print("old tokens")
                    # print(request.session["auth_tokens"])

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

                    # print(request.session["auth_tokens"])
                    request.session["auth_tokens"] = tokens
                    # print("new tokens: ")
                    # print(request.session["auth_tokens"])

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
                    contextData = {
                        "timeout": True
                    }
                    return render(request, "signin.html", context=contextData)

        except Exception as exception:
            traceback.print_exception(Exception, exception, exception.__traceback__)
            
            # response = {
            #     "message": "failed to sign up - duplicate username",
            #     "status": 400
            # }

            return render(request, "signin.html")
    elif(request.method == "POST"):
        print(request.data)

        postBody = {}

        if("change_email" in request.data):
            postBody["user_email"] = request.data["change_email"]
        if("change_username" in request.data):
            postBody["user_name"] = request.data["change_username"]
        if("change_password" in request.data):
            postBody["password"] = request.data["change_password"]

        try:

            headerDef = {
                "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"],
                "Content-Type": "application/json"
            }

            # print(headerDef)

            userId = str(request.session["user_id"])

            res = eureka_client.do_service("USER-SERVICE", "/user/" + userId + "/", method="PUT", headers=headerDef, data=postBody)
            
            resJson = json.loads(res)

            # formattedUser = resJson["user-details"]

            # user = {
            #     # "username": request.session["username"],

            #     "user": formattedUser
            # }
            # print("response from update call")
            # print(resJson)

            request.session["username"] = resJson["user-details"]["user_name"]

            return JsonResponse(resJson)
        except HTTPError as httpError:
            if(httpError.code == 403 or httpError.code == 500):
                try:
                    print(httpError.code)


                    headerDef = {
                        "Authorization": "Bearer " + request.session["auth_tokens"]["refresh_token"],
                        "Content-Type": "application/json"
                    }            
                    # print(headerDef)
                    # print("old tokens")
                    # print(request.session["auth_tokens"])

                    print("doing refresh call")
                    res = eureka_client.do_service("USER-SERVICE", "/user/refreshAuth", method="GET", headers=headerDef)

                    print("successful refresh call")
                    authJson = json.loads(res)

                    # print("json response: ", authJson)
                    tokens =  {
                        "access_token": authJson["access_token"],
                        #FOR SOME REASON THIS ONE COMES IN WITH A UNDERSCORE.
                        "refresh_token": authJson["refresh_token"]
                    }

                    request.session["auth_tokens"] = None

                    # print(request.session["auth_tokens"])
                    request.session["auth_tokens"] = tokens
                    # print("new tokens: ")
                    # print(request.session["auth_tokens"])

                    headerDef = {
                        "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"],
                        "Content-Type": "application/json"
                    }

                    res = eureka_client.do_service("USER-SERVICE", "/user/" + request.session["user_id"], method="PUT", headers=headerDef, data=postBody)
                    resJson = json.loads(res)
                    # formattedUser = resJson["user-details"]
                    # print("response from update call")
                    # print(resJson)
                    request.session["username"] = resJson["user-details"]["user_name"]

                    return JsonResponse(resJson)
                except Exception as exception:
                    traceback.print_exception(Exception, exception, exception.__traceback__)
                    responseBody = {
                        "message":"failed",
                        "exc": traceback.format_exc()
                    }
                    return JsonResponse(responseBody)


@api_view(("GET",))
def userDash(request, *args, **kwargs):
    print("hit userdash endpoint")
    try:
        # on hitting this do a eureka call to get this up to date as possible
        headerDef = {
            "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
        }
        updateRes = eureka_client.do_service("DJANGO-SOCKET-SERVER", "/getStatus/", method="GET", headers=headerDef)
        print(json.loads(updateRes))

        serializer = mainapp.models.UserSerializer(mainapp.models.User.objects.get(username=request.session["username"]))

        printerStatus = serializer.data

        print(printerStatus)

        statusList = printerStatus["consumers"]

        jsonStatusList = json.loads(statusList)

        print(jsonStatusList)

        # for status in statusList:
        context ={
            "username": request.session["username"],
            "auth":request.session["auth"], 
            "status": jsonStatusList
        }

        headerDef = {
            "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
        }

        # print(headerDef)

        username = str(request.session["username"])

        res = eureka_client.do_service("FILE-SERVICE", "/file/getFiles/" + username + "/", method="GET", headers=headerDef)
        
        resJson = json.loads(res)

        files = resJson["files"]

        context["files"] = files

        return render(request, "dashboard.html", context=context)
    except HTTPError as httpException:
        traceback.print_exception(HTTPError, httpException, httpException.__traceback__)
        if(httpException.code == 403):
            try:

                print(httpException.code)
                headerDef = {
                    "Authorization": "Bearer " + request.session["auth_tokens"]["refresh_token"],
                    "Content-Type": "application/json"
                }            
                # print(headerDef)
                # print("old tokens")
                # print(request.session["auth_tokens"])

                print("doing refresh call")
                res = eureka_client.do_service("USER-SERVICE", "/user/refreshAuth", method="GET", headers=headerDef)

                print("successful refresh call")
                authJson = json.loads(res)

                # print("json response: ", authJson)
                tokens =  {
                    "access_token": authJson["access_token"],
                    #FOR SOME REASON THIS ONE COMES IN WITH A UNDERSCORE.
                    "refresh_token": authJson["refresh_token"]
                }

                
                request.session["auth_tokens"] = None

                # print(request.session["auth_tokens"])
                request.session["auth_tokens"] = tokens

                headerDef = {
                    "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
                }
                updateRes = eureka_client.do_service("DJANGO-SOCKET-SERVER", "/getStatus/", method="GET", headers=headerDef)
                print(json.loads(updateRes))

                serializer = mainapp.models.UserSerializer(mainapp.models.User.objects.get(username=request.session["username"]))

                printerStatus = serializer.data

                print(printerStatus)

                statusList = printerStatus["consumers"]

                jsonStatusList = json.loads(statusList)

                print(jsonStatusList)

                # for status in statusList:
                context ={
                    "username": request.session["username"],
                    "auth":request.session["auth"], 
                    "status": jsonStatusList
                }

                headerDef = {
                    "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
                }

                # print(headerDef)

                username = str(request.session["username"])

                res = eureka_client.do_service("USER-SERVICE", "/file/getFiles/" + username + "/", method="GET", headers=headerDef)
                
                resJson = json.loads(res)

                files = resJson["files"]

                context["files"] = files
                
                return render(request, "dashboard.html", context=context)
            except HTTPError as nestedHTTPException:
                traceback.print_exception(HTTPError, nestedHTTPException, nestedHTTPException.__traceback__)
                return render(request, "signin.html")
            except ObjectDoesNotExist as userNotFound:
                traceback.print_exception(ObjectDoesNotExist, userNotFound, userNotFound.__traceback__)
                return render(request, "register.html")
        else:
            return render(request, "signin.html")
    except Exception as exception:
        traceback.print_exception(Exception, exception, exception.__traceback__)
        return render(request, "signin.html")


@api_view(("GET", ))
def getUserFileList(request, *args, **kwargs):
    try:
        username = request.session["username"]

        headerDef = {
            "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
        }

        # print(headerDef)

        username = str(request.session["username"])

        res = eureka_client.do_service("FILE-SERVICE", "/file/getFiles/" + username + "/", method="GET", headers=headerDef)

        resJson = json.loads(res)

        resToSend = {
            "message": "File list for user " + username + "returned",
            "files": resJson["files"],
            "username": username
        }
        return JsonResponse(resToSend)
    except HTTPError as httpException:
        traceback.print_exception(HTTPError, httpException, httpException.__traceback__)
        if(httpException.code == 403):
            try:

                print(httpException.code)
                headerDef = {
                    "Authorization": "Bearer " + request.session["auth_tokens"]["refresh_token"],
                    "Content-Type": "application/json"
                }            
                # print(headerDef)
                # print("old tokens")
                # print(request.session["auth_tokens"])

                print("doing refresh call")
                res = eureka_client.do_service("USER-SERVICE", "/user/refreshAuth", method="GET", headers=headerDef)

                print("successful refresh call")
                authJson = json.loads(res)

                # print("json response: ", authJson)
                tokens =  {
                    "access_token": authJson["access_token"],
                    #FOR SOME REASON THIS ONE COMES IN WITH A UNDERSCORE.
                    "refresh_token": authJson["refresh_token"]
                }

                
                request.session["auth_tokens"] = None

                # print(request.session["auth_tokens"])
                request.session["auth_tokens"] = tokens

                res = eureka_client.do_service("FILE-SERVICE", "/file/getFiles/" + username + "/", method="GET", headers=headerDef)

                resJson = json.loads(res)

                resToSend = {
                    "message": "File list for user " + username + "returned",
                    "files": resJson["files"],
                    "username": username
                }
                return JsonResponse(resToSend)
            except Exception as exception:
                traceback.print_exception(Exception, exception, exception.__traceback__)
                
                response = {
                    "message": "failed to get filelist"
                }
                
                return JsonResponse(response)
        else:
            traceback.print_exception(Exception, exception, exception.__traceback__)
                
            response = {
                "message": "failed to get filelist"
            }
            
            return JsonResponse(response)
    except Exception as exception:
        traceback.print_exception(Exception, exception, exception.__traceback__)
        
        response = {
            "message": "failed to get filelist"
        }
        
        return JsonResponse(response)


@api_view(("GET", ))
def getUserPrinterList(request, *args, **kwargs):
    try:
        serializer = mainapp.models.UserSerializer(mainapp.models.User.objects.get(username=request.session["username"]))

        printerStatus = serializer.data

        print(printerStatus)

        statusList = printerStatus["consumers"]

        jsonStatusList = json.loads(statusList)

        print(jsonStatusList)

        printerList = []

        for item in jsonStatusList:
            print(item)
            printerList.append(item["printer_name"])

        jsonResponse = {
            "message": "Printers for user " + request.session["username"] + " returned",
            "printers": printerList
        }

        print(jsonResponse)

        return JsonResponse(jsonResponse)
    except Exception as exception:
        traceback.print_exception(Exception, exception, exception.__traceback__)
        exceptionResponse = {
            "message": "failed to retrieve printers for user"
        }

        print(exceptionResponse)

        return JsonResponse(exceptionResponse)


@api_view(("POST", ))
def queuePrint(request, *args, **kwargs):
    try:

        data = request.data
        file = data["file"]
        printer = data["printer"]
        userauth = request.session["auth_tokens"]["access_token"]


        resToSend = {
            "file": file,
            "printer": printer
        }

        headerDef = {
            "Authorization": userauth,
            "Content-Type": "application/json"
        }            

        res = eureka_client.do_service("DJANGO-SOCKET-SERVER", "/queuePrint/", method="POST", headers=headerDef, data=resToSend)

        jsonRes = json.loads(res)

        print(jsonRes)
        return JsonResponse(jsonRes)
    except HTTPError as httpError:
        traceback.print_exception(HTTPError, httpError, httpError.__traceback__)
        try:
            print(httpError.code)
            headerDef = {
                "Authorization": "Bearer " + request.session["auth_tokens"]["refresh_token"],
                "Content-Type": "application/json"
            }            
            # print(headerDef)
            # print("old tokens")
            # print(request.session["auth_tokens"])

            print("doing refresh call")
            res = eureka_client.do_service("USER-SERVICE", "/user/refreshAuth", method="GET", headers=headerDef)

            print("successful refresh call")
            authJson = json.loads(res)

            # print("json response: ", authJson)
            tokens =  {
                "access_token": authJson["access_token"],
                #FOR SOME REASON THIS ONE COMES IN WITH A UNDERSCORE.
                "refresh_token": authJson["refresh_token"]
            }

            
            request.session["auth_tokens"] = None

            # print(request.session["auth_tokens"])
            request.session["auth_tokens"] = tokens


            data = request.data
            file = data["file"]
            printer = data["printer"]
            userauth = request.session["auth_tokens"]["access_token"]


            resToSend = {
                "file": file,
                "printer": printer
            }

            headerDef = {
                "Authorization": userauth,
                "Content-Type": "application/json"
            }  

            res = eureka_client.do_service("DJANGO-SOCKET-SERVER", "/queuePrint/", method="POST", headers=headerDef, data=resToSend)

            jsonRes = json.loads(res)

            print(jsonRes)
            return JsonResponse(jsonRes)
        except Exception as exception:
            traceback.print_exception(Exception, exception, exception.__traceback__)
            jsonRes = {
                "message": "failed to queue print"
            }

            return JsonResponse(jsonRes)
# Also nonfunctional - throws 415
@api_view(("POST",))
def addFileForUser(request, *args, **kwargs):
    print("hit addfile request")
    try:
        file = request.FILES["file"]
        username = request.session["username"]

        postData = {
            "file": file,
            "username": username
        }

        headerDef = {
            "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
        }

        res = eureka_client.do_service("FILE-SERVICE", method="POST", headers=headerDef, data=postData)

        print(res)

        return JsonResponse(res)
    except HTTPError as httpError:
        try:
            traceback.print_exception(HTTPError, httpError, httpError.__traceback__)

            headerDef = {
                "Authorization": "Bearer " + request.session["auth_tokens"]["refresh_token"],
                "Content-Type": "application/json"
            }            
            # print(headerDef)
            # print("old tokens")
            # print(request.session["auth_tokens"])

            print("doing refresh call")
            res = eureka_client.do_service("USER-SERVICE", "/user/refreshAuth", method="GET", headers=headerDef)

            print("successful refresh call")
            authJson = json.loads(res)

            # print("json response: ", authJson)
            tokens =  {
                "access_token": authJson["access_token"],
                #FOR SOME REASON THIS ONE COMES IN WITH A UNDERSCORE.
                "refresh_token": authJson["refresh_token"]
            }

            
            request.session["auth_tokens"] = None

            # print(request.session["auth_tokens"])
            request.session["auth_tokens"] = tokens

            file = request.FILES["file"]
            username = request.session["username"]

            postData = {
                "file": file,
                "username": username
            }

            headerDef = {
                "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
            }

            res = eureka_client.do_service("FILE-SERVICE", method="POST", headers=headerDef, data=postData)

            print(res)

            return JsonResponse(res)
        except Exception as exception:
            traceback.print_exception(Exception, exception, exception.__traceback__)
            errorResponse = {
                "message": "failed to add file"
            }
            return JsonResponse(errorResponse)

# Nonfunctional - keeps throwing 403s for no reason
# # Required parsers to handle posted files
# class FileAdd(APIView):
#     parser_classes = (MultiPartParser, FormParser,)
#     def post(request, *args, **kwargs):
#         print("hit addfile request")
#         try:
#             file = request.FILES["file"]
#             username = request.session["username"]

#             postData = {
#                 "file": file,
#                 "username": username
#             }

#             headerDef = {
#                 "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
#             }

#             res = eureka_client.do_service("FILE-SERVICE", method="POST", headers=headerDef, data=postData)

#             print(res)

#             return JsonResponse(res)
#         except HTTPError as httpError:
#             try:
#                 traceback.print_exception(HTTPError, httpError, httpError.__traceback__)

#                 headerDef = {
#                     "Authorization": "Bearer " + request.session["auth_tokens"]["refresh_token"],
#                     "Content-Type": "application/json"
#                 }            
#                 # print(headerDef)
#                 # print("old tokens")
#                 # print(request.session["auth_tokens"])

#                 print("doing refresh call")
#                 res = eureka_client.do_service("USER-SERVICE", "/user/refreshAuth", method="GET", headers=headerDef)

#                 print("successful refresh call")
#                 authJson = json.loads(res)

#                 # print("json response: ", authJson)
#                 tokens =  {
#                     "access_token": authJson["access_token"],
#                     #FOR SOME REASON THIS ONE COMES IN WITH A UNDERSCORE.
#                     "refresh_token": authJson["refresh_token"]
#                 }

                
#                 request.session["auth_tokens"] = None

#                 # print(request.session["auth_tokens"])
#                 request.session["auth_tokens"] = tokens

#                 file = request.FILES["file"]
#                 username = request.session["username"]

#                 postData = {
#                     "file": file,
#                     "username": username
#                 }

#                 headerDef = {
#                     "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"]
#                 }

#                 res = eureka_client.do_service("FILE-SERVICE", method="POST", headers=headerDef, data=postData)

#                 print(res)

#                 return JsonResponse(res)
#             except Exception as exception:
#                 traceback.print_exception(Exception, exception, exception.__traceback__)
#                 errorResponse = {
#                     "message": "failed to add file"
#                 }
#                 return JsonResponse(errorResponse)
# Need to implement - will do later
@api_view(("DELETE",))
def deleteFile(request, *args, **kwargs):
    print("delete endpoint hit")
    try:
        data = request.data

        filename = data["filename"]
        username = request.session["username"]


        headerDef = {
            "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"],
        }            

        res = eureka_client.do_service("FILE-SERVICE", "/file/deleteFile/" + username + "/" + filename + "/", method="DELETE", headers=headerDef)

        print(json.loads(res))

        return JsonResponse(json.loads(res))
    except HTTPError as httpError:
        traceback.print_exception(HTTPError, httpError, httpError.__traceback__)
        try:
            headerDef = {
                "Authorization": "Bearer " + request.session["auth_tokens"]["refresh_token"],
                "Content-Type": "application/json"
            }            
            # print(headerDef)
            # print("old tokens")
            # print(request.session["auth_tokens"])

            print("doing refresh call")
            res = eureka_client.do_service("USER-SERVICE", "/user/refreshAuth", method="GET", headers=headerDef)

            print("successful refresh call")
            authJson = json.loads(res)

            # print("json response: ", authJson)
            tokens =  {
                "access_token": authJson["access_token"],
                #FOR SOME REASON THIS ONE COMES IN WITH A UNDERSCORE.
                "refresh_token": authJson["refresh_token"]
            }

            
            request.session["auth_tokens"] = None

            # print(request.session["auth_tokens"])
            request.session["auth_tokens"] = tokens

            data = request.data

            filename = data["filename"]
            username = request.session["username"]


            headerDef = {
                "Authorization": "Bearer " + request.session["auth_tokens"]["access_token"],
            }            

            res = eureka_client.do_service("FILE-SERVICE", "/file/" + username + "/" + filename + "/", method="DELETE", headers=headerDef)

            print(json.loads(res))

            return JsonResponse(json.loads(res))

        except Exception as exception:
            traceback.print_exception(Exception, exception, exception.__traceback__)
            errorResponse = {
                "message" : "failed to delete file"
            }
            return JsonResponse(errorResponse)
    except Exception as exception:
        traceback.print_exception(Exception, exception, exception.__traceback__)
        errorResponse = {
            "message" : "failed to delete file"
        }
        return JsonResponse(errorResponse)