
# Import functions and objects the microservice needs.
# - Flask is the top-level application. You implement the application by adding methods to it.
# - Response enables creating well-formed HTTP/REST responses.
# - requests enables accessing the elements of an incoming HTTP/REST request.
#

import json
import copy
import pandas as pd
from datetime import datetime

from flask import Flask, Response, render_template
from flask import request
from flask_table import Table, Col

from resources import query_execute as C

_default_limit = 10


application = Flask(__name__)


##################################################################################################################

def _get_and_remove_arg(args, arg_name):

    val = copy.copy(args.get(arg_name, None))
    if val is not None:
        del args[arg_name]

    return args, val


def _de_array_args(args):

    result = {}

    if args is not None:
        for k,v in args.items():
            result[k] = ",".join(v)

    return result


# 1. Extract the input information from the requests object.
# 2. Log the information
# 3. Return extracted information.
#
def log_and_extract_input(method, path_params=None):

    path = request.path
    args = dict(request.args)
    args = _de_array_args(args)
    data = None
    headers = dict(request.headers)
    method = request.method

    args, limit = _get_and_remove_arg(args, "limit")
    args, offset = _get_and_remove_arg(args, "offset")
    args, order_by = _get_and_remove_arg(args, "order_by")
    args, fields = _get_and_remove_arg(args, "fields")

    args = _de_array_args(args)

    if limit is None:
        limit = _default_limit

    try:
        if request.data is not None:
            data = request.json
        else:
            data = None
    except Exception as e:
        # This would fail the request in a more real solution.
        data = "You sent something but I could not get JSON out of it."

    log_message = str(datetime.now()) + ": Method " + method

    inputs =  {
        "path": path,
        "method": method,
        "path_params": path_params,
        "query_params": args,
        "headers": headers,
        "body": data,
        "limit": limit,
        "offset": offset,
        "order_by": order_by,
        "url": request.url,
        "base_url": request.base_url,
        "fields": fields
        }

    log_message += " received: \n" + json.dumps(inputs, indent=2)
    print(log_message)

    return inputs


def log_response(method, status, data, txt):

    msg = {
        "method": method,
        "status": status,
        "txt": txt,
        "data": data
    }

    print(str(datetime.now()) + ": \n" + json.dumps(msg, indent=2, default=str))


# This function performs a basic health check. We will flesh this out.
@application.route("/api/health", methods=["GET"])
def health_check():

    rsp_data = { "status": "healthy", "time": str(datetime.now()) }
    rsp = Response(rsp_data, status=200, content_type="application/json")
    return rsp


@application.route("/api/demo/<parameter>", methods=["GET", "POST"])
def demo(parameter):

    inputs = log_and_extract_input(demo, { "parameter": parameter })

    msg = {
        "/demo received the following inputs" : inputs
    }

    rsp = Response(json.dumps(msg), status=200, content_type="application/json")
    return rsp


@application.route("/api/customer/<cc_num>", methods=["GET"])
def get_character_by_id(cc_num):

    res = C.get_costumer_by_id(cc_num)
    df = pd.DataFrame([res], columns=res.keys())
    return render_template("customer.html", tables=[df.to_html(index=False)], titles=df.columns.values, cc_num=cc_num)

    # rsp = Response(json.dumps(res), status=200, content_type="application/json")
    # return rsp


@application.route("/api/statement/<cc_num>", methods=["GET"])
def get_statement_by_id(cc_num):

    res = C.get_statement_by_id(cc_num)
    df = pd.DataFrame(res["data"])
    df.sort_values(by="trans_time")
    return render_template("statement.html", tables=[df.to_html(index=False)], titles=df.columns.values, cc_num=cc_num)

    # rsp = Response(json.dumps(res), status=200, content_type="application/json")
    # return rsp


# run the app.
if __name__ == "__main__":
    # Setting debug to True enables debug output. This line should be
    # removed before deploying a production app.

    application.debug = True
    application.run(host='0.0.0.0', port=5050)