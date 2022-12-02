FROM python
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV EUREKA_HOST="eureka"
ENV EUREKA_PORT=8761
ENV JWT_SECRET="bruh"
ENV MONGO_HOST="printerdb"
ENV MONGO_PORT=27017

# ENV SOCKET_IO_PORT=5001
COPY requirements.txt .
RUN pip install -r requirements.txt 
ADD . .

# RUN python Scripts/mainsite/manage.py makemigrations

# RUN python Scripts/mainsite/manage.py migrate

CMD python Scripts/mainsite/manage.py runserver 0.0.0.0:8000