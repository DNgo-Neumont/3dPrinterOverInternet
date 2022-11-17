
from py_eureka_client import eureka_client
import os

os.environ.setdefault("EUREKA_HOST", "localhost")
os.environ.setdefault("EUREKA_PORT", "8761")

eurekaHost = os.environ["EUREKA_HOST"]
eurekaPort = os.environ["EUREKA_PORT"]

eureka_client.init(f"http://{eurekaHost}:{eurekaPort}", app_name="django-frontend",instance_port=8000)