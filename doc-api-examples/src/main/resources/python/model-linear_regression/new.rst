Consider the following model trained and tested on the sample data set in *frame* 'frame'.

<hide>
>>> import trustedanalytics as ta
>>> ta.connect()
-etc-
>>> frame = ta.Frame(ta.UploadRows([[0,0],[1, 2.5],[2, 5.0],[3, 7.5],[4, 10],[5, 12.5],[6, 13.0],[7, 17.15], [8, 18.5],[9, 23.5]],[("x1", ta.float64),("y", ta.float64)]))
-etc-

</hide>
Consider the following frame containing two columns.

>>> frame.inspect()
[#]  x1   y
===============
[0]  0.0    0.0
[1]  1.0    2.5
[2]  2.0    5.0
[3]  3.0    7.5
[4]  4.0   10.0
[5]  5.0   12.5
[6]  6.0   13.0
[7]  7.0  17.15
[8]  8.0   18.5
[9]  9.0   23.5

>>> model = ta.LinearRegressionModel()
<progress>
>>> train_output = model.train(frame,'y',['x1'])
<progress>
<skip>
>>> train_output
{u'explained_variance': 49.27592803030301,
 u'intercept': -0.032727272727271384,
 u'iterations': 3,
 u'label': u'y',
 u'mean_absolute_error': 0.5299393939393939,
 u'mean_squared_error': 0.6300969696969692,
 u'objective_history': [0.5, 0.007324606455391047, 0.006312834669731454],
 u'observation_columns': [u'x1'],
 u'r_2': 0.9873743306605371,
 u'root_mean_squared_error': 0.7937864761363531,
 u'weights': [2.4439393939393934]}
</skip>
>>> test_output = model.test(frame,'y')
<progress>
<skip>
>>> test_output
{u'explained_variance': 49.27592803030301,
 u'mean_absolute_error': 0.5299393939393939,
 u'mean_squared_error': 0.6300969696969692,
 u'r_2': 0.9873743306605371,
 u'root_mean_squared_error': 0.7937864761363531}
</skip>
>>> predicted_frame = model.predict(frame, ["x1"])
<progress>
<skip>
>>> predicted_frame.inspect()
[#]  x1   y      predicted_value
==================================
[0]  4.0   10.0      9.74303030303
[1]  0.0    0.0   -0.0327272727273
[2]  1.0    2.5      2.41121212121
[3]  6.0   13.0      14.6309090909
[4]  3.0    7.5      7.29909090909
[5]  7.0  17.15      17.0748484848
[6]  9.0   23.5      21.9627272727
[7]  8.0   18.5      19.5187878788
[8]  5.0   12.5       12.186969697
[9]  2.0    5.0      4.85515151515

</skip>
>>> model.publish()
<progress>
<skip>
"hdfs://HOST:PORT/user/atkuser/models_5a4f72ab41c54c8c85cfc981c2952905.tar"
</skip>
<skip>
>>> import requests
>>> headers = {'Content-type': 'application/json', 'Accept': 'application/json,text/plain'}

# posting a request to get the metadata about the model
>>> r =requests.get('http://my-svm-model.demotrustedanalytics.com/v2/metadata')
>>> r.text
u'{"model_details":{"model_type":"LibSvm Model","model_class":"org.trustedanalytics.atk.scoring.models.LibSvmModel","model_reader":"org.trustedanalytics.atk.scoring.models.LibSvmModelReaderPlugin","custom_values":{}},"input":[{"name":"tr_row","value":"Double"},{"name":"tr_col","value":"Double"}],"output":[{"name":"tr_row","value":"Double"},{"name":"tr_col","value":"Double"},{"name":"Prediction","value":"Double"}]}'

# Posting a request to version 1 of Scoring Engine supporting strings for requests and response:
>>> r = requests.post('http://my-svm-model.demotrustedanalytics.com/v1/score?data=2,17,-6', headers=headers)
>>> r.text
u'-1.0'

# Posting a request to version 1 with multiple records to score:
>>> r = requests.post('http://my-svm-model.demotrustedanalytics.com/v1/score?data=2,17,-6&data=0,0,0', headers=headers)
>>> r.text
u'-1.0,1.0'

# Posting a request to version 2 of Scoring Engine supporting Json for requests and responses. In the following example, 'tr_row' and 'tr_col' are the names of the observation columns that the model was trained on:
>>> r = requests.post("http://my-svm-model.demotrustedanalytics.com/v2/score", json={"records": [{"tr_row": 1.0, "tr_col": 2.6}]})
>>> r.text
u'{"data":[{"tr_row":1.0,"tr_col":2.6,"Prediction":-1.0}]}'

# posting a request to version 2 with multiple records to score:
>>> r = requests.post("http://my-svm-model.demotrustedanalytics.com/v2/score", json={"records": [{"tr_row": 1.0, "tr_col": 2.6},{"tr_row": 3.0, "tr_col": 0.6} ]})
>>> r.text
u'{"data":[{"tr_row":1.0,"tr_col":2.6,"Prediction":-1.0},{"tr_row":3.0,"tr_col":0.6,"Prediction":-1.0}]}'
</skip>

