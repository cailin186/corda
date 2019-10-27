#include "MapReader.h"

#include "proton/proton_wrapper.h"

/******************************************************************************/

amqp::internal::schema::Restricted::RestrictedTypes
amqp::internal::reader::
MapReader::restrictedType() const {
    return schema::Restricted::Restricted::Map;
}

/******************************************************************************/

std::list<uPtr<amqp::reader::IValue>>
amqp::internal::reader::
MapReader::dump_(
    pn_data_t *,
    const SchemaType &
) const {

}

/******************************************************************************/

std::unique_ptr<amqp::reader::IValue>
amqp::internal::reader::
MapReader::dump(
        const std::string & name_,
        pn_data_t * data_,
        const SchemaType & schema_
) const {
    proton::auto_next an (data_);

    return std::make_unique<TypedPair<sList<uPtr<amqp::reader::IValue>>>>(
            name_,
            dump_ (data_, schema_));
}

/******************************************************************************/

std::unique_ptr<amqp::reader::IValue>
amqp::internal::reader::
MapReader::dump(
        pn_data_t * data_,
        const SchemaType & schema_
) const  {
    proton::auto_next an (data_);

    return std::make_unique<TypedSingle<sList<uPtr<amqp::reader::IValue>>>>(
            dump_ (data_, schema_));
}

/******************************************************************************/
