import boto3
import json
import time

client = boto3.client('kinesis')

while True:
    data = {
        "pim_id": 123,
        "vet": "Dr. Smith",
        "clinic": "Oak Vet",
        "timestamp": time.time()
    }

    response = client.put_record(
        StreamName='pims-raw-data',
        Data=json.dumps(data),
        PartitionKey="pim_123"
    )
    print("Sent:", data)
    time.sleep(3)