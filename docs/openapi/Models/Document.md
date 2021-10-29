# Document
## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**attributes** | [**List**](Attribute.md) | multiple attributes can be delivered separately from standard data fields | [optional] [default to null]
**categories** | [**List**](array.md) |  | [optional] [default to null]
**data** | **Map** | The data property should be used for standard fields, such as title, description, price. Only values of the following types are accepted (others will be dropped silently): Standard primitive types (Boolean, String, Integer, Double) and arrays of these types. Attributes (key-value objects with ID) should be passed to the attributes property. | [default to null]
**id** | **String** |  | [default to null]

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
