from cassandra.cluster import Cluster
import copy
from datetime import datetime

cluster = Cluster()
session = cluster.connect("creditcard")

def get_by_query(table_name, args):

    q_string = dict(copy.copy(args))
    terms = []

    for k,v in q_string.items():
        terms.append(str(k) + "='" + str(v) + "'")

    if len(terms) > 0:
        wc = "WHERE " + " AND ".join(terms)
    else:
        wc = ""

    q = "SELECT * FROM " + table_name + " " + wc;

    rows = session.execute(q)
    print("Query = " + q)
    return rows


def get_costumer_by_id(cc_num):
    args = {"cc_num": cc_num}
    row = get_by_query("customer", args)
    age = int((datetime.today() - row.one().dob).days / 365.2425)

    res = {
        "cc_num": row.one().cc_num,
        "first_name": row.one().first,
        "last_name": row.one().last,
        "gender": row.one().gender,
        "age": age,
        "job": row.one().job,
        "street": row.one().street,
        "city": row.one().city,
        "state": row.one().state,
        "zipcode": row.one().zip,
    }

    return res


def get_statement_by_id(cc_num):
    args = {"cc_num": cc_num}
    rows = get_by_query("non_fraud_transaction", args)
    data = []
    for row in rows:
        r = {
            "cc_num": row.cc_num,
            "trans_num": row.trans_num,
            "trans_time": str(row.trans_time),
            "trans_amount": row.amt,
            "category": row.category,
            "merchant": row.merchant,
            "distance": row.distance
        }
        data.append(r)

    link = {
        "name": "customer",
        "href": f"http://0.0.0.0:5050/api/customer/{cc_num}"
    }

    res = {
        "data": data,
        "link": link
    }

    return res