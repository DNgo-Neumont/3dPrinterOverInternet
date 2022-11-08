FROM python
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV EUREKA_HOST="eureka"
ENV EUREKA_PORT=8761
ENV JWT_SECRET="bruh"
# ENV SOCKET_IO_PORT=5001
COPY requirements.txt .
RUN pip install -r requirements.txt 
ADD . .

CMD python Scripts/socketservercapstone/manage.py runserver