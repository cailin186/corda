#pragma once

#include "Restricted.h"

/******************************************************************************/

namespace amqp::internal::schema {

    class Map;
    class Enum;

}

/******************************************************************************/

namespace amqp::internal::schema {

    class List : public Restricted {
        public :
            static std::pair<std::string, std::string> listType (
                    const std::string &);

        private :
            std::vector<std::string> m_listOf;

            int dependsOnMap (const class Map &) const override;
            int dependsOnList (const class List &) const override;
            int dependsOnEnum (const class Enum &) const override;

        public :
            List (
                uPtr<Descriptor> &,
                const std::string &,
                const std::string &,
                const std::vector<std::string> &,
                const std::string &);

            std::vector<std::string>::const_iterator begin() const override;
            std::vector<std::string>::const_iterator end() const override;

            const std::string & listOf() const;

            int dependsOn (const class Composite &) const override;
            int dependsOn (const class Restricted & lhs_) const override {
                return Restricted::dependsOn (lhs_);
            }
    };

}

/******************************************************************************/
