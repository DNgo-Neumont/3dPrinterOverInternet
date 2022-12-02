from django.db import models
from rest_framework import serializers
from djongo import models


class Status(models.Model):
    # _id = models.ObjectIdField(primary_key=True)
    username = models.CharField(primary_key=True, max_length=2048)
    printer_name = models.CharField(max_length=8096)
    printer_progress = models.IntegerField()


# class Consumer(models.Model):
#     # _id = models.ObjectIdField(primary_key=True)

#     username = models.CharField(primary_key=True, max_length=2048)
#     status = models.ArrayField(
#         model_container=Status
#     )


# Create your models here.
class User(models.Model):
    _id = models.ObjectIdField()
    username = models.CharField(max_length=2048)
    consumers = models.ArrayField(
        model_container=Status
    )




class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ["_id","username", "consumers"]

# class ConsumerSerializer(serializers.ModelSerializer):
#     class Meta:
#         model = Consumer
#         fields = ["status"]

class StatusSerializer(serializers.ModelSerializer):
    class Meta:
        model = Status
        fields = ["printer_name", "printer_progress"]
