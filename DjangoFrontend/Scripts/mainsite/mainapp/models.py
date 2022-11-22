from django.db import models
from rest_framework import serializers
from djongo import models

class Consumer(models.Model):
    name = models.CharField(max_length=2048)


# Create your models here.
class User(models.Model):
    _id = models.ObjectIdField()
    username = models.CharField(max_length=2048)




class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ["_id","username"]