from django.shortcuts import render
from django.http.response import HttpResponse
from django.template import loader
from django.template import Template
from rest_framework.decorators import api_view

# Create your views here.
@api_view(("GET",))
def index(request):
    print(request)
    template = loader.get_template("index.html")
    return HttpResponse(template.render())

@api_view(("GET", "POST"))
def login_render(request, *args, **kwargs):
    print(request)

    if(request.method == "GET"):
        template = loader.get_template("signin.html")
        return HttpResponse(template.render())
    elif(request.method == "POST"):
        print(request.data)

        # body = dict(request.body)

        # print(body)

        print(request)
        print(args)
        print(kwargs)

        template = loader.get_template("index.html")
        return HttpResponse(template.render())